package pe.edu.upc.ice.track.platform.monitoring.infrastructure.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Declares the thread pool the telemetry stream writes to its clients on.
 *
 * <p>Writing to a Server-Sent Events connection blocks while the client's socket buffer is full, so
 * a slow or stalled browser must never hold an ingestion request thread, nor the single scheduler
 * thread. Every stream write therefore runs here: the after-commit fan-out of readings and alert
 * changes, and the periodic heartbeat. The open connections themselves hold no thread at all; they
 * are parked by the servlet container's asynchronous support.</p>
 *
 * <p>The queue is bounded. When it is full the oldest queued update is dropped, with a warning,
 * rather than the newest: a live dashboard is better served by fresh data, and it reloads the
 * history over the REST API when it reconnects.</p>
 *
 * <p>Declaring an executor bean makes Spring Boot back off its own {@code applicationTaskExecutor}
 * unless {@code spring.task.execution.mode=force} is set, which it is in {@code application.properties}.</p>
 */
@Configuration
@Slf4j
public class StreamingExecutorConfiguration {

  /**
   * Creates the stream write pool.
   *
   * <p>The name is referenced verbatim by {@code @Async("streamTaskExecutor")} on
   * {@code TelemetryStreamEventDispatcher} and by {@code TelemetryStreamRegistry}; rename them
   * together.</p>
   *
   * @param corePoolSize  threads kept alive
   * @param maxPoolSize   threads at most, added only once the queue is full
   * @param queueCapacity updates waiting for a thread at most
   * @return the executor
   */
  @Bean(name = "streamTaskExecutor")
  public ThreadPoolTaskExecutor streamTaskExecutor(
      @Value("${monitoring.telemetry-stream.executor.core-size:4}") int corePoolSize,
      @Value("${monitoring.telemetry-stream.executor.max-size:8}") int maxPoolSize,
      @Value("${monitoring.telemetry-stream.executor.queue-capacity:1000}") int queueCapacity) {
    var executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("stream-");
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy() {
      @Override
      public void rejectedExecution(Runnable task, ThreadPoolExecutor pool) {
        log.warn("Telemetry stream pool saturated ({} queued); dropping the oldest pending update",
            pool.getQueue().size());
        super.rejectedExecution(task, pool);
      }
    });
    // Pending pushes are worthless once the connections are closing.
    executor.setWaitForTasksToCompleteOnShutdown(false);
    return executor;
  }
}
