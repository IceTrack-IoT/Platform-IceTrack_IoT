package pe.edu.upc.ice.track.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Boostrap class for the IceTrackPlatform application.
 *
 * <p>Initializes Spring Boot autoconfiguration and JPA auditing infrastructure.</p>
 */
@SpringBootApplication
@EnableJpaAuditing
public class IceTrackPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(IceTrackPlatformApplication.class, args);
	}

}
