package pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the open owner event streams, partitioned by the owner profile each one was opened for,
 * writes events to them, and keeps a short replay buffer per owner so a reconnecting browser misses
 * nothing.
 *
 * <p><strong>Tenant isolation.</strong> Emitters live in {@code emittersByOwner}, keyed by owner
 * profile identifier, and {@link #publish} writes only to the emitters stored under the identifier it
 * is given. An emitter is stored under exactly one owner, the one its stream ticket was issued for,
 * so an event can never reach another owner's stream. The replay buffers are keyed the same way.</p>
 *
 * <p><strong>Event identifiers and replay.</strong> Every event gets an SSE {@code id} of the form
 * {@code <boot>-<sequence>}: the instant this instance started, in epoch milliseconds, and a sequence
 * that increases per owner. The last {@value #REPLAY_CAPACITY} events of the last
 * {@link #REPLAY_WINDOW} are kept per owner, whichever is fewer, while the owner has a stream open and
 * for {@link #REPLAY_WINDOW} after its last one closed. A stream opened with the identifier of the last
 * event its client saw first receives every buffered event after it. {@code stream-opened} reports
 * how many were replayed and whether the replay is complete: it is not when the identifier is older
 * than the buffer, unknown, or was issued by another instance or before a restart, and the client must
 * then reload over the REST API.</p>
 *
 * <p><strong>Concurrency.</strong> Each owner's emitter set is immutable and replaced atomically,
 * through {@link ConcurrentHashMap#compute}, whenever an emitter is added or removed (copy on write;
 * an owner holds a handful of streams at most). Publishing an event and opening a stream for the same
 * owner are serialized on that owner's buffer, so a new stream receives the replay and the live events
 * after it in order, and never the same event twice.</p>
 *
 * <p><strong>Lifecycle.</strong> Every emitter is dropped on completion, on timeout (after
 * {@link #STREAM_TIMEOUT}, when it is also completed, so the browser reconnects with a new ticket), on
 * a container-reported error, and as soon as a write to it fails. A heartbeat comment written every
 * {@code monitoring.telemetry-stream.heartbeat-ms} keeps proxies from closing idle connections and
 * detects clients that went away silently; the same task drops the buffers of owners whose last stream
 * closed more than {@link #REPLAY_WINDOW} ago. An owner holds at most
 * {@code monitoring.telemetry-stream.max-streams-per-owner} streams; opening one more completes the
 * oldest, which is usually a tab that was closed without the server noticing yet.</p>
 *
 * <p>Heartbeats run on the {@code streamTaskExecutor} pool, never on a scheduler thread. The registry
 * lives in this instance's memory: with several replicas, events must be fanned out between them, for
 * example through PostgreSQL {@code LISTEN/NOTIFY} or Redis pub/sub, and a replay is only complete on
 * the instance that issued the identifier.</p>
 */
@Component
@Slf4j
public class TelemetryStreamRegistry {

  /** How long a stream stays open before the browser has to reconnect. */
  public static final Duration STREAM_TIMEOUT = Duration.ofMinutes(30);

  /** Name of the event written first on every stream, once it is registered. */
  public static final String STREAM_OPENED_EVENT = "stream-opened";

  /** How long events are kept for replay. */
  public static final Duration REPLAY_WINDOW = Duration.ofMinutes(5);

  /** Most events kept for replay per owner. */
  public static final int REPLAY_CAPACITY = 1_000;

  /** Delay the browser waits before reconnecting a dropped stream, sent as the SSE {@code retry} field. */
  static final long RECONNECT_DELAY_MS = 5_000;

  /** Prefix of every event identifier this instance issues: the instant it started. */
  private final String bootPrefix = String.valueOf(System.currentTimeMillis());

  private final Map<Long, Set<SseEmitter>> emittersByOwner = new ConcurrentHashMap<>();
  private final Map<Long, OwnerEventLog> logsByOwner = new ConcurrentHashMap<>();
  private final int maxStreamsPerOwner;
  private final long heartbeatMillis;
  private final TaskExecutor streamTaskExecutor;

  /**
   * Creates the registry.
   *
   * @param maxStreamsPerOwner streams one owner may keep open at once; at least 1
   * @param heartbeatMillis    interval between two heartbeats, in milliseconds
   * @param streamTaskExecutor the pool heartbeats run on
   */
  public TelemetryStreamRegistry(
      @Value("${monitoring.telemetry-stream.max-streams-per-owner:10}") int maxStreamsPerOwner,
      @Value("${monitoring.telemetry-stream.heartbeat-ms:25000}") long heartbeatMillis,
      @Qualifier("streamTaskExecutor") TaskExecutor streamTaskExecutor) {
    if (maxStreamsPerOwner < 1) {
      throw new IllegalArgumentException("monitoring.telemetry-stream.max-streams-per-owner must be at least 1");
    }
    this.maxStreamsPerOwner = maxStreamsPerOwner;
    this.heartbeatMillis = heartbeatMillis;
    this.streamTaskExecutor = Objects.requireNonNull(streamTaskExecutor, "streamTaskExecutor must not be null");
  }

  /**
   * Opens a stream for an owner without replay.
   *
   * @param ownerProfileId identifier of the owner profile the stream is opened for; required
   * @return the emitter to return from the controller
   */
  public SseEmitter subscribe(Long ownerProfileId) {
    return subscribe(ownerProfileId, null);
  }

  /**
   * Opens a stream for an owner, replaying the buffered events its client has not seen.
   *
   * <p>The emitter is registered before it is returned. Its first event, {@value #STREAM_OPENED_EVENT},
   * then the replayed events, are queued until the response is initialized, so the browser knows at
   * once that the stream is live, how long to wait before reconnecting, and whether it missed
   * anything.</p>
   *
   * @param ownerProfileId identifier of the owner profile the stream is opened for; required
   * @param lastEventId    identifier of the last event the client saw, or {@code null} for a stream
   *                       opened right after a fresh load
   * @return the emitter to return from the controller
   */
  public SseEmitter subscribe(Long ownerProfileId, String lastEventId) {
    Objects.requireNonNull(ownerProfileId, "ownerProfileId must not be null");
    var emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
    var eventLog = logsByOwner.computeIfAbsent(ownerProfileId, id -> new OwnerEventLog());
    synchronized (eventLog) {
      eventLog.touch();
      register(ownerProfileId, emitter);
      var replay = eventLog.replayAfter(lastEventId, bootPrefix, Instant.now());
      var opened = new LinkedHashMap<String, Object>();
      opened.put("timeout_seconds", STREAM_TIMEOUT.toSeconds());
      opened.put("heartbeat_seconds", Duration.ofMillis(heartbeatMillis).toSeconds());
      opened.put("replayed", replay.events().size());
      opened.put("replay_complete", replay.complete());
      send(ownerProfileId, emitter, SseEmitter.event()
          .name(STREAM_OPENED_EVENT)
          .reconnectTime(RECONNECT_DELAY_MS)
          .data(opened));
      for (var event : replay.events()) {
        send(ownerProfileId, emitter, event.toSse());
      }
      log.debug("Owner {} opened a stream ({} open, {} events replayed, complete: {})",
          ownerProfileId, streamCount(ownerProfileId), replay.events().size(), replay.complete());
    }
    return emitter;
  }

  /**
   * Writes an event to every stream of one owner, and to no other, and keeps it for replay.
   *
   * <p>Nothing is kept for an owner who has no stream open and closed none within
   * {@link #REPLAY_WINDOW}: nobody could ask for its replay.</p>
   *
   * @param ownerProfileId identifier of the owner profile the event belongs to; {@code null} writes nothing
   * @param eventName      the SSE event name
   * @param payload        the event data, serialized as JSON
   */
  public void publish(Long ownerProfileId, String eventName, Object payload) {
    if (ownerProfileId == null) {
      return;
    }
    var eventLog = logsByOwner.get(ownerProfileId);
    if (eventLog == null) {
      return;
    }
    synchronized (eventLog) {
      var event = eventLog.append(bootPrefix, eventName, payload, Instant.now());
      var emitters = emittersByOwner.get(ownerProfileId);
      if (emitters == null) {
        return;
      }
      for (var emitter : emitters) {
        // A builder cannot be shared: building an event appends its terminator to it.
        send(ownerProfileId, emitter, event.toSse());
      }
    }
  }

  /**
   * Tells whether any owner has a stream open on this instance, or closed one within
   * {@link #REPLAY_WINDOW}, so a caller can skip the work of preparing an event nobody would receive.
   *
   * @return {@code true} when at least one owner is followed
   */
  public boolean hasSubscribers() {
    return !logsByOwner.isEmpty();
  }

  /**
   * Tells whether an owner has a stream open on this instance, or closed one within
   * {@link #REPLAY_WINDOW}: whether an event of the owner would be delivered or kept for replay.
   *
   * @param ownerProfileId identifier of the owner profile
   * @return {@code true} when events of the owner are followed
   */
  public boolean isFollowed(Long ownerProfileId) {
    return ownerProfileId != null && logsByOwner.containsKey(ownerProfileId);
  }

  /**
   * @param ownerProfileId identifier of the owner profile
   * @return how many streams the owner has open on this instance
   */
  public int streamCount(Long ownerProfileId) {
    var emitters = ownerProfileId == null ? null : emittersByOwner.get(ownerProfileId);
    return emitters == null ? 0 : emitters.size();
  }

  /**
   * Writes a heartbeat comment to every open stream, on the stream pool, as a single task, so the
   * heartbeat can neither block the scheduler thread nor crowd real updates out of the pool's queue.
   * Drops first the replay buffers nobody can ask for any more.
   */
  @Scheduled(
      fixedDelayString = "${monitoring.telemetry-stream.heartbeat-ms:25000}",
      initialDelayString = "${monitoring.telemetry-stream.heartbeat-ms:25000}")
  public void sendHeartbeats() {
    dropIdleLogs(Instant.now());
    if (emittersByOwner.isEmpty()) {
      return;
    }
    var snapshot = Map.copyOf(emittersByOwner);
    try {
      streamTaskExecutor.execute(() -> snapshot.forEach((ownerProfileId, emitters) ->
          emitters.forEach(emitter -> send(ownerProfileId, emitter, SseEmitter.event().comment("heartbeat")))));
    } catch (TaskRejectedException e) {
      log.debug("Telemetry stream heartbeat skipped: {}", e.getMessage());
    }
  }

  /**
   * Completes every open stream when the application stops, so browsers reconnect to another
   * instance instead of waiting for a timeout.
   */
  @PreDestroy
  public void completeAll() {
    var snapshot = Map.copyOf(emittersByOwner);
    emittersByOwner.clear();
    logsByOwner.clear();
    snapshot.values().forEach(emitters -> emitters.forEach(emitter -> {
      try {
        emitter.complete();
      } catch (RuntimeException e) {
        log.debug("Could not complete a telemetry stream on shutdown: {}", e.getMessage());
      }
    }));
  }

  /**
   * Registers an emitter under an owner and wires its cleanup callbacks. Completes the owner's oldest
   * streams beyond the per-owner limit.
   *
   * @param ownerProfileId identifier of the owner profile; required
   * @param emitter        the emitter to register; required
   */
  void register(Long ownerProfileId, SseEmitter emitter) {
    Objects.requireNonNull(ownerProfileId, "ownerProfileId must not be null");
    Objects.requireNonNull(emitter, "emitter must not be null");
    emitter.onCompletion(() -> unregister(ownerProfileId, emitter));
    emitter.onTimeout(() -> {
      unregister(ownerProfileId, emitter);
      // Completing here ends the request normally; otherwise the timeout would surface as an error.
      emitter.complete();
    });
    emitter.onError(error -> unregister(ownerProfileId, emitter));

    var evicted = new ArrayList<SseEmitter>();
    emittersByOwner.compute(ownerProfileId, (id, current) -> {
      var next = new LinkedHashSet<SseEmitter>(current == null ? Set.of() : current);
      next.add(emitter);
      var oldestFirst = next.iterator();
      while (next.size() > maxStreamsPerOwner && oldestFirst.hasNext()) {
        evicted.add(oldestFirst.next());
        oldestFirst.remove();
      }
      return Collections.unmodifiableSet(next);
    });
    evicted.forEach(oldest -> {
      log.debug("Owner {} exceeded {} telemetry streams; completing the oldest", ownerProfileId, maxStreamsPerOwner);
      oldest.complete();
    });
  }

  /**
   * Removes an emitter from its owner's set, dropping the owner's entry once it is empty, and starts
   * the owner's replay window.
   *
   * @param ownerProfileId identifier of the owner profile the emitter was registered under
   * @param emitter        the emitter to remove
   */
  private void unregister(Long ownerProfileId, SseEmitter emitter) {
    emittersByOwner.computeIfPresent(ownerProfileId, (id, current) -> {
      if (!current.contains(emitter)) {
        return current;
      }
      var next = new LinkedHashSet<SseEmitter>(current);
      next.remove(emitter);
      return next.isEmpty() ? null : Collections.unmodifiableSet(next);
    });
    var eventLog = logsByOwner.get(ownerProfileId);
    if (eventLog != null) {
      eventLog.touch();
    }
  }

  /**
   * Drops the replay buffer of every owner who has no stream open and whose last stream closed more
   * than {@link #REPLAY_WINDOW} ago.
   *
   * @param now the current instant
   */
  private void dropIdleLogs(Instant now) {
    logsByOwner.entrySet().removeIf(entry ->
        !emittersByOwner.containsKey(entry.getKey()) && entry.getValue().isIdleSince(now.minus(REPLAY_WINDOW)));
  }

  /**
   * Writes one event to one emitter, dropping the emitter when the write fails.
   *
   * <p>An {@link IOException} means the client went away; the container reports it through the
   * emitter's error and completion callbacks as well, so the emitter is not completed here. An
   * {@link IllegalStateException} means the emitter already completed. In both cases nothing more is
   * sent to it.</p>
   */
  private void send(Long ownerProfileId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
    try {
      emitter.send(event);
    } catch (IOException | IllegalStateException e) {
      unregister(ownerProfileId, emitter);
      log.debug("Dropped a telemetry stream of owner {}: {}", ownerProfileId, e.getMessage());
    }
  }

  /**
   * One event kept for replay.
   *
   * @param id          its SSE identifier
   * @param sequence    its position in the owner's sequence
   * @param name        its SSE event name
   * @param payload     its data
   * @param publishedAt when it was published
   */
  private record BufferedEvent(String id, long sequence, String name, Object payload, Instant publishedAt) {

    SseEmitter.SseEventBuilder toSse() {
      return SseEmitter.event().id(id).name(name).data(payload);
    }
  }

  /**
   * The events to send a stream that just opened, and whether they are all it missed.
   *
   * @param events   the events after the client's last one, oldest first
   * @param complete {@code false} when events the client missed are no longer buffered
   */
  private record Replay(List<BufferedEvent> events, boolean complete) {
  }

  /**
   * The sequence and replay buffer of one owner. Every method is called while holding the instance's
   * monitor, except {@link #touch()} and {@link #isIdleSince}, which only read or write a volatile
   * instant.
   */
  private static final class OwnerEventLog {

    private final ArrayDeque<BufferedEvent> events = new ArrayDeque<>();
    private long sequence;
    private volatile Instant lastActivityAt = Instant.now();

    /** Records that a stream of the owner opened or closed now. */
    void touch() {
      lastActivityAt = Instant.now();
    }

    /**
     * @param cutoff an instant
     * @return {@code true} when no stream of the owner opened or closed since the cutoff
     */
    boolean isIdleSince(Instant cutoff) {
      return lastActivityAt.isBefore(cutoff);
    }

    /**
     * Gives the next event of the owner its identifier and keeps it.
     *
     * @return the kept event
     */
    BufferedEvent append(String bootPrefix, String name, Object payload, Instant now) {
      sequence++;
      var event = new BufferedEvent(bootPrefix + "-" + sequence, sequence, name, payload, now);
      events.addLast(event);
      prune(now);
      return event;
    }

    /**
     * Collects the buffered events after the one a client saw last.
     *
     * @param lastEventId the identifier the client sent, or {@code null}
     * @param bootPrefix  the prefix of the identifiers this instance issues
     * @param now         the current instant
     * @return the events to replay, and whether nothing the client missed was lost
     */
    Replay replayAfter(String lastEventId, String bootPrefix, Instant now) {
      prune(now);
      if (lastEventId == null || lastEventId.isBlank()) {
        return new Replay(List.of(), true);
      }
      var seen = parseSequence(lastEventId.strip(), bootPrefix);
      if (seen < 0 || seen > sequence) {
        return new Replay(List.of(), false);
      }
      var oldestKept = events.isEmpty() ? sequence + 1 : events.peekFirst().sequence();
      var complete = seen >= oldestKept - 1;
      var missed = events.stream().filter(event -> event.sequence() > seen).toList();
      return new Replay(missed, complete);
    }

    /** Drops the events older than the replay window, and the oldest beyond the capacity. */
    private void prune(Instant now) {
      var cutoff = now.minus(REPLAY_WINDOW);
      while (!events.isEmpty()
          && (events.size() > REPLAY_CAPACITY || events.peekFirst().publishedAt().isBefore(cutoff))) {
        events.removeFirst();
      }
    }

    /**
     * @return the sequence an identifier carries, or {@code -1} when it was not issued by this
     *         instance since it started
     */
    private static long parseSequence(String eventId, String bootPrefix) {
      var separator = eventId.lastIndexOf('-');
      if (separator <= 0 || !eventId.substring(0, separator).equals(bootPrefix)) {
        return -1;
      }
      try {
        return Long.parseLong(eventId.substring(separator + 1));
      } catch (NumberFormatException e) {
        return -1;
      }
    }
  }
}
