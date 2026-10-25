package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportDownloadResource;

/**
 * Converts a {@link ReportDownloadLink} into a {@link ReportDownloadResource}.
 */
public class ReportDownloadResourceFromLinkAssembler {

  private ReportDownloadResourceFromLinkAssembler() {
  }

  /**
   * @param link the link to convert
   * @return the equivalent resource
   */
  public static ReportDownloadResource toResourceFromLink(ReportDownloadLink link) {
    return new ReportDownloadResource(link.url(), link.expiresInSeconds());
  }
}
