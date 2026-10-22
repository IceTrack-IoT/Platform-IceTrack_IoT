package pe.edu.upc.ice.track.platform.assets.infrastructure.geocoding.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Declares the thread pool geocoding lookups run on.
 *
 * <p>A dedicated, bounded pool acts as a bulkhead: a slow or rate-limited provider can only ever
 * occupy these few threads, never the request threads or any other asynchronous work. The queue
 * absorbs bursts such as a bulk import or the startup recovery; if it ever fills up, the lookup is
 * rejected, the site simply stays {@code PENDING}, and the owner's request is unaffected because it
 * has already been answered.</p>
 *
 * <p>Declaring any {@code Executor} bean normally makes Spring Boot's auto-configured
 * {@code applicationTaskExecutor} back off, which would leave this pool as the implicit default
 * for every unqualified {@code @Async} in the platform. {@code spring.task.execution.mode=force}
 * in {@code application.properties} prevents that: Boot keeps its own executor as the default,
 * and this one is only ever reached by name.</p>
 */
@Configuration
public class GeocodingExecutorConfiguration {

  /**
   * Creates the geocoding thread pool.
   *
   * <p>The name is referenced verbatim by {@code @Async("geocodingTaskExecutor")} on
   * {@code SiteGeocodingEventHandler}, which as application code cannot import a constant from
   * this class; rename both together. A mismatch does not fail at startup - Spring resolves the
   * executor only when the first lookup is dispatched, after the owner's transaction committed, and
   * then merely logs the error while every site stays {@code PENDING}.</p>
   *
   * @return the executor
   */
  @Bean(name = "geocodingTaskExecutor")
  public ThreadPoolTaskExecutor geocodingTaskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("geocoding-");
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    // Lets in-flight lookups finish on a graceful shutdown; whatever is still queued afterward is
    // resumed by the startup recovery, since those sites are still PENDING.
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(15);
    return executor;
  }
}
