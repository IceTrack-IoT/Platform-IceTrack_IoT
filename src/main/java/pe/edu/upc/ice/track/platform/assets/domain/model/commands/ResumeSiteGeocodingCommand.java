package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command, issued in the background, to continue locating a site left {@code PENDING} because the
 * maps service could not answer right away.
 *
 * @param siteId       identifier of the pending site; required
 * @param finalAttempt {@code true} when no attempt follows this one, so a provider that still cannot
 *                     answer leaves the site {@code FAILED} instead of {@code PENDING}
 */
public record ResumeSiteGeocodingCommand(Long siteId, boolean finalAttempt) {
}
