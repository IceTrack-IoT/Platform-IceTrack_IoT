package pe.edu.upc.ice.track.platform.reporting.infrastructure.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Declares the thread pool report generation runs on.
 *
 * <p>A small, bounded pool keeps generation, which reads telemetry and renders files, away from the
 * request threads and from every other asynchronous task. The queue holds the reports requested while
 * the pool is busy; a report rejected by a full queue stays {@code PENDING}.</p>
 *
 * <p>Declaring an executor bean makes Spring Boot back off its own {@code applicationTaskExecutor}
 * unless {@code spring.task.execution.mode=force} is set, which it is in {@code application.properties}.</p>
 */
@Configuration
public class ReportingExecutorConfiguration {

  /**
   * Creates the report generation pool.
   *
   * <p>The name is referenced verbatim by {@code @Async("reportTaskExecutor")} on
   * {@code ReportGenerationWorker}; rename both together.</p>
   *
   * @return the executor
   */
  @Bean(name = "reportTaskExecutor")
  public ThreadPoolTaskExecutor reportTaskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("report-");
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(100);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    return executor;
  }
}
