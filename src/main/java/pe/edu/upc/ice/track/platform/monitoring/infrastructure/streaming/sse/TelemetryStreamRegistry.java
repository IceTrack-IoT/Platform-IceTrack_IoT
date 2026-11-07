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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the open telemetry streams, partitioned by the owner profile each one was opened for, and
 * writes events to them.
 *
 * <p><strong>Tenant isolation.</strong> Emitters live in {@code emittersByOwner}, keyed by owner
 * profile identifier, and {@link #publish} writes only to the emitters stored under the identifier it
 * is given. An emitter is stored under exactly one owner, the one its stream ticket was issued for,
 * so an event can never reach another owner's stream.</p>
 *
 * <p><strong>Concurrency.</strong> Each owner's set is immutable and replaced atomically, through
 * {@link ConcurrentHashMap#compute}, whenever an emitter is added or removed (copy on write; an owner
 * holds a handful of streams at most). Publishing iterates a snapshot without locking, and
 * {@link SseEmitter#send} serializes concurrent writes to the same emitter itself.</p>
 *
 * <p><strong>Lifecycle.</strong> Every emitter is dropped on completion, on timeout (after
 * {@link #STREAM_TIMEOUT}, when it is also completed, so the browser reconnects with a new ticket), on
 * a container-reported error, and as soon as a write to it fails. A heartbeat comment written every
 * {@code monitoring.telemetry-stream.heartbeat-ms} keeps proxies from closing idle connections and
 * detects clients that went away silently. An owner holds at most
 * {@code monitoring.telemetry-stream.max-streams-per-owner} streams; opening one more completes the
 * oldest, which is usually a tab that was closed without the server noticing yet.</p>
 *
 * <p>Writes run on the {@code streamTaskExecutor} pool, never on a request or scheduler thread. The
 * registry lives in this instance's memory: with several replicas, events must be fanned out between
 * them, for example through PostgreSQL {@code LISTEN/NOTIFY} or Redis pub/sub.</p>
 */
@Component
@Slf4j
public class TelemetryStreamRegistry {

  /** How long a stream stays open before the browser has to reconnect. */
  public static final Duration STREAM_TIMEOUT = Duration.ofMinutes(30);

  /** Name of the event written first on every stream, once it is registered. */
  public static final String STREAM_OPENED_EVENT = "stream-opened";

  /** Delay the browser waits before reconnecting a dropped stream, sent as the SSE {@code retry} field. */
  static final long RECONNECT_DELAY_MS = 5_000;

  private final Map<Long, Set<SseEmitter>> emittersByOwner = new ConcurrentHashMap<>();
  private final int maxStreamsPerOwner;
  private final long heartbeatMillis;
  private final TaskExecutor streamTaskExecutor;

  /**
   * Creates the registry.
   *
   * @param maxStreamsPerOwner streams one owner may keep open at once; at least 1
   * @param heartbeatMillis    interval between two heartbeats, in milliseconds
   * @param streamTaskExecutor the pool stream writes run on
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
   * Opens a stream for an owner.
   *
   * <p>The emitter is registered before it is returned, and its first event,
   * {@value #STREAM_OPENED_EVENT}, is queued until the response is initialized, so the browser knows
   * at once that the stream is live and how long to wait before reconnecting.</p>
   *
   * @param ownerProfileId identifier of the owner profile the stream is opened for; required
   * @return the emitter to return from the controller
   */
  public SseEmitter subscribe(Long ownerProfileId) {
    var emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
    register(ownerProfileId, emitter);
    send(ownerProfileId, emitter, SseEmitter.event()
        .name(STREAM_OPENED_EVENT)
        .reconnectTime(RECONNECT_DELAY_MS)
        .data(Map.of(
            "timeout_seconds", STREAM_TIMEOUT.toSeconds(),
            "heartbeat_seconds", Duration.ofMillis(heartbeatMillis).toSeconds())));
    log.debug("Owner {} opened a telemetry stream ({} open for this owner)", ownerProfileId, streamCount(ownerProfileId));
    return emitter;
  }

  /**
   * Writes an event to every stream of one owner, and to no other.
   *
   * @param ownerProfileId identifier of the owner profile the event belongs to; {@code null} writes nothing
   * @param eventName      the SSE event name
   * @param payload        the event data, serialized as JSON
   */
  public void publish(Long ownerProfileId, String eventName, Object payload) {
    if (ownerProfileId == null) {
      return;
    }
    var emitters = emittersByOwner.get(ownerProfileId);
    if (emitters == null) {
      return;
    }
    for (var emitter : emitters) {
      // A builder cannot be shared: building an event appends its terminator to it.
      send(ownerProfileId, emitter, SseEmitter.event().name(eventName).data(payload));
    }
  }

  /**
   * Tells whether any stream is open on this instance, so a caller can skip the work of preparing an
   * event nobody would receive.
   *
   * @return {@code true} when at least one owner has an open stream
   */
  public boolean hasSubscribers() {
    return !emittersByOwner.isEmpty();
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
   */
  @Scheduled(
      fixedDelayString = "${monitoring.telemetry-stream.heartbeat-ms:25000}",
      initialDelayString = "${monitoring.telemetry-stream.heartbeat-ms:25000}")
  public void sendHeartbeats() {
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
   * Removes an emitter from its owner's set, dropping the owner's entry once it is empty.
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
}
