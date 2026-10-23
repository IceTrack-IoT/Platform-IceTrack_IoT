package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

/**
 * Command to review one of the caller's completed service requests.
 *
 * @param serviceRequestId identifier of the request; required
 * @param username         username of the authenticated caller, resolved by the command service to
 *                         the owner the request must belong to; required
 * @param communication    communication score, from 1 to 5; required
 * @param efficacy         efficacy score, from 1 to 5; required
 * @param performance      performance score, from 1 to 5; required
 * @param comment          free-text comment, may be {@code null}
 */
public record SubmitReviewCommand(
    Long serviceRequestId,
    String username,
    Integer communication,
    Integer efficacy,
    Integer performance,
    String comment) {
}
