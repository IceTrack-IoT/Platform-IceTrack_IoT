package pe.edu.upc.ice.track.platform.shared.infrastructure.async.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's {@code @Async} support for every bounded context.
 *
 * <p>Asynchronous handlers live in their own bounded context and name the executor they run on,
 * this class only switches the infrastructure on.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
}
