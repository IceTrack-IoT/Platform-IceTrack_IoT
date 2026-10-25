package pe.edu.upc.ice.track.platform.reporting.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.ice.track.platform.reporting.application.commandservices.ReportCommandService;
import pe.edu.upc.ice.track.platform.reporting.application.queryservices.ReportQueryService;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportDownloadResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.RequestReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform.ReportDownloadResourceFromLinkAssembler;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform.ReportResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform.RequestReportCommandFromResourceAssembler;

import java.util.List;

/**
 * REST controller exposing the Reporting & Análisis bounded context.
 *
 * <p>Report generation is requested, not polled for on-demand: {@code POST /api/v1/reports}
 * submits a {@link pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand},
 * which is run synchronously against the matching
 * {@link pe.edu.upc.ice.track.platform.reporting.application.internal.strategies.ReportGenerationStrategy}
 * before the response is returned, so the response already carries the report's final status
 * (see {@code ReportCommandServiceImpl}'s Javadoc for why this is synchronous).</p>
 */
@RestController
@RequestMapping(value = "/api/v1/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reports", description = "Available Report Endpoints")
public class ReportsController {

  private final ReportCommandService reportCommandService;
  private final ReportQueryService reportQueryService;

  public ReportsController(ReportCommandService reportCommandService, ReportQueryService reportQueryService) {
    this.reportCommandService = reportCommandService;
    this.reportQueryService = reportQueryService;
  }

  /**
   *  Requests a new report, generating it synchronously before responding.
   * @param resource the report request
   * @return the generated report, already {@code COMPLETED} or {@code FAILED}
   */
  @PostMapping
  @Operation(summary = "Request a new report",
      description = "Submits a report request, generates it synchronously through the matching "
          + "Strategy implementation, and returns it in its final state.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Report generated",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid report request")
  })
  public ResponseEntity<ReportResource> requestReport(@RequestBody RequestReportResource resource) {
    var command = RequestReportCommandFromResourceAssembler.toCommandFromResource(resource);
    Report report = reportCommandService.handle(command);
    var reportResource = ReportResourceFromEntityAssembler.toResourceFromEntity(report);
    return new ResponseEntity<>(reportResource, HttpStatus.CREATED);
  }

  /**
   *  Retrieves a single report by its identifier.
   * @param reportId the identifier of the report to retrieve
   * @return the report, or 404 when not found
   */
  @GetMapping("/{reportId}")
  @Operation(summary = "Get a report by id",
      description = "Retrieves a single, already-generated report by its identifier.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Report found",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "404", description = "Report not found")
  })
  public ResponseEntity<ReportResource> getReportById(
      @Parameter(description = "Identifier of the report", example = "1", required = true)
      @PathVariable Long reportId) {
    return reportQueryService.handle(new GetReportByIdQuery(reportId))
        .map(ReportResourceFromEntityAssembler::toResourceFromEntity)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   *  Lists every report a user has requested, most recent first.
   * @param userId the identifier of the requesting user
   * @return the user's reports, empty when they have none
   */
  @GetMapping
  @Operation(summary = "List a user's reports",
      description = "Retrieves every report a given user has requested, most recent first.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Reports retrieved",
          content = @Content(schema = @Schema(implementation = ReportResource.class)))
  })
  public ResponseEntity<List<ReportResource>> getReportsByUser(
      @Parameter(description = "Identifier of the requesting user", example = "1", required = true)
      @RequestParam Long userId) {
    List<Report> reports = reportQueryService.handle(new GetReportsByUserQuery(userId));
    List<ReportResource> resources = reports.stream()
        .map(ReportResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(resources);
  }

  /**
   *  Obtains a temporary link to download a report's file.
   * @param reportId the identifier of the report
   * @param userId   the identifier of the user asking; must be the report's requester
   * @return the temporary download link, or 404 when it is not available to this user
   */
  @GetMapping("/{reportId}/download")
  @Operation(summary = "Get a temporary download link for a report's file",
      description = "Returns a time-limited URL to download the report's rendered file straight "
          + "from the file storage. Only the user who requested the report can obtain it, and only "
          + "once the report is COMPLETED. A new link must be requested after it expires.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Download link generated",
          content = @Content(schema = @Schema(implementation = ReportDownloadResource.class))),
      @ApiResponse(responseCode = "404",
          description = "Report not found, requested by another user, or without a file")
  })
  public ResponseEntity<ReportDownloadResource> getReportDownloadLink(
      @Parameter(description = "Identifier of the report", example = "1", required = true)
      @PathVariable Long reportId,
      @Parameter(description = "Identifier of the requesting user", example = "1", required = true)
      @RequestParam Long userId) {
    return reportQueryService.handle(new GetReportDownloadLinkQuery(reportId, userId))
        .map(ReportDownloadResourceFromLinkAssembler::toResourceFromLink)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
