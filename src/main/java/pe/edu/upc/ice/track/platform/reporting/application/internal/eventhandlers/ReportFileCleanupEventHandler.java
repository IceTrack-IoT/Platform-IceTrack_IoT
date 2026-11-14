package pe.edu.upc.ice.track.platform.reporting.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportDeletedEvent;

/**
 * Removes the stored file of a deleted report, once the deletion has committed.
 *
 * <p>Any signed link issued for the file stops working as soon as the object is gone. A file that
 * cannot be removed is logged and left behind: the report is already deleted, and an orphan object in
 * the private bucket is reachable by nobody.</p>
 */
@Service
@Slf4j
public class ReportFileCleanupEventHandler {

  private final ReportFileStorage fileStorage;

  public ReportFileCleanupEventHandler(ReportFileStorage fileStorage) {
    this.fileStorage = fileStorage;
  }

  /**
   * Deletes the file of the report.
   *
   * @param event the event naming the report and its file
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(ReportDeletedEvent event) {
    if (event.fileKey() == null) {
      return;
    }
    try {
      fileStorage.delete(event.fileKey());
      log.info("Removed the file {} of deleted report {}", event.fileKey(), event.reportId());
    } catch (RuntimeException ex) {
      log.warn("The file {} of deleted report {} could not be removed", event.fileKey(), event.reportId(), ex);
    }
  }
}
