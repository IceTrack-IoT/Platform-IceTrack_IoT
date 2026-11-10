package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * A temporary link to download the file of a completed report.
 *
 * @param downloadUrl signed URL; the browser saves the file under {@code fileName}
 * @param expiresAt   when the signature expires
 * @param fileName    suggested file name, extension included
 * @param contentType MIME type of the file
 * @param sizeBytes   size of the file in bytes
 */
@Schema(
    name = "ReportDownloadLinkResponse",
    description = "Temporary link to download the file of a completed report",
    example = "{\"download_url\": \"https://storage.googleapis.com/icetrack-reports/reports/7/8.pdf?X-Goog-Signature=abc\", "
        + "\"expires_at\": \"2026-11-09T15:15:00Z\", \"file_name\": \"excursions-october-2026.pdf\", "
        + "\"content_type\": \"application/pdf\", \"size_bytes\": 184320}")
public record ReportDownloadLinkResource(
    @Schema(description = "Signed download URL; the browser saves the file under file_name",
        example = "https://storage.googleapis.com/icetrack-reports/reports/7/8.pdf?X-Goog-Signature=abc")
    String downloadUrl,

    @Schema(description = "When the signature expires", example = "2026-11-09T15:15:00Z")
    Instant expiresAt,

    @Schema(description = "Suggested file name, extension included", example = "excursions-october-2026.pdf")
    String fileName,

    @Schema(description = "MIME type of the file", example = "application/pdf",
        allowableValues = {"application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"})
    String contentType,

    @Schema(description = "Size of the file in bytes", example = "184320")
    long sizeBytes
) {
}
