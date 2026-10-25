package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

/**
 * Value object representing a temporary link to download a report's file.
 *
 * <p>It is computed on demand and never persisted: the link stops working after
 * {@code expiresInSeconds}, so a new one is requested each time the user wants the file.</p>
 *
 * @param url              the temporary download URL
 * @param expiresInSeconds how long the URL stays valid, in seconds
 */
public record ReportDownloadLink(String url, long expiresInSeconds) {
}
