package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource carrying a temporary link to download a report's file.
 *
 * @param downloadUrl      the temporary URL; open it directly, no authentication header needed
 * @param expiresInSeconds how many seconds the URL keeps working; request a new one afterwards
 */
@Schema(
    name = "ReportDownloadResponse",
    description = "Temporary link to download a generated report",
    example = "{\"download_url\": \"https://storage.example.com/reports/42.pdf?signature=abc\", "
        + "\"expires_in_seconds\": 900}"
)
public record ReportDownloadResource(
    @Schema(description = "Temporary URL; open it directly, no authentication header needed",
        example = "https://storage.example.com/reports/42.pdf?signature=abc")
    String downloadUrl,

    @Schema(description = "How many seconds the URL keeps working; request a new one afterwards",
        example = "900")
    long expiresInSeconds) {
}
