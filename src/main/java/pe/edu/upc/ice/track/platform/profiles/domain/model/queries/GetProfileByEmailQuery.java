package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;

/**
 * Get Profile By Email Query
 */
public record GetProfileByEmailQuery(EmailAddress email) {
}
