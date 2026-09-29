package pe.edu.upc.ice.track.platform.shared.infrastructure.scheduling.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} support for every bounded context.
 *
 * <p>Scheduled handlers live in their own bounded context, such as the IAM refresh token purge;
 * this class only switches the scheduling infrastructure on.</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
