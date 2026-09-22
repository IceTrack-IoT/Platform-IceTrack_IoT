package pe.edu.upc.ice.track.platform.iam.infrastructure.hashing.bcrypt;

import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.hashing.HashingService;

/**
 * This interface is a marker interface for the BCrypt hashing service.
 * It extends the {@link HashingService} and {@link PasswordEncoder} interfaces.
 * This interface is used to inject the BCrypt hashing service in the {@link pe.edu.upc.ice.track.platform.iam.infrastructure.hashing.bcrypt.services.HashingServiceImpl} class.
 */
public interface BCryptHashingService extends HashingService, PasswordEncoder {
}
