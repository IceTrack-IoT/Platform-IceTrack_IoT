package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

/**
 * Command to edit one of the caller's reviews within its edit window.
 *
 * @param reviewId      identifier of the review; required
 * @param username      username of the authenticated caller, resolved by the command service to the
 *                      owner who must have written the review; required
 * @param communication new communication score, from 1 to 5; required
 * @param efficacy      new efficacy score, from 1 to 5; required
 * @param performance   new performance score, from 1 to 5; required
 * @param comment       new free-text comment, may be {@code null}
 */
public record UpdateReviewCommand(
    Long reviewId,
    String username,
    Integer communication,
    Integer efficacy,
    Integer performance,
    String comment) {
}
