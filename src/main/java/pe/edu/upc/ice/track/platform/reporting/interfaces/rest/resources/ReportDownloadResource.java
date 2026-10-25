package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

/**
 * Resource carrying a temporary link to download a report's file.
 *
 * @param downloadUrl      the temporary URL; open it directly, no authentication header needed
 * @param expiresInSeconds how many seconds the URL keeps working; request a new one afterwards
 */
public record ReportDownloadResource(String downloadUrl, long expiresInSeconds) {
}
