package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to dismiss an alert as not actionable, without asserting the underlying
 * condition was resolved.
 *
 * <p>Owner-only: the command service applies it only when the caller owns the unit the
 * alert was raised on, and otherwise answers as if the alert did not exist. The dismissing user
 * is recorded as the authenticated caller, never taken from the request body.</p>
 *
 * @param alertId  identifier of the alert to dismiss
 * @param username username of the authenticated caller; required
 * @param reason   why the alert is not actionable, such as a planned defrost cycle; required
 * @param evidence supporting evidence, such as a note or a link; optional
 */
public record DismissAlertCommand(Long alertId, String username, String reason, String evidence) {

  /**
   * Validates that a reason was supplied.
   *
   * @throws IllegalArgumentException when the reason is missing or blank
   */
  public DismissAlertCommand {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("A reason is required to dismiss an alert");
    }
  }
}
