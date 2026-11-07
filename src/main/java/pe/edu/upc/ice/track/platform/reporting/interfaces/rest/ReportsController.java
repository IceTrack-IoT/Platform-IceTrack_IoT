package pe.edu.upc.ice.track.platform.reporting.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

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
 *
 * <p>Every endpoint is scoped to the authenticated caller. The {@code user_id} body field
 * and {@code userId} query parameter older clients still send are accepted but ignored: a report
 * of anybody else answers 404, exactly like a missing one.</p>
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
   * @param authentication the authenticated principal, who becomes the report's requester
   * @return the generated report, already {@code COMPLETED} or {@code FAILED}
   */
  @PostMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Request a new report",
      description = "Submits a report request on behalf of the authenticated owner, generates it "
          + "synchronously through the matching Strategy implementation, and returns it in its final "
          + "state. A site or unit filter must belong to the caller.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Report generated",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid report request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "The requested site or unit is not reachable by the caller")
  })
  public ResponseEntity<?> requestReport(
      @RequestBody RequestReportResource resource, Authentication authentication) {
    var command = RequestReportCommandFromResourceAssembler.toCommandFromResource(resource, authentication.getName());
    return ResponseEntityAssembler.toResponseEntityFromResult(
        reportCommandService.handle(command),
        ReportResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   *  Retrieves a single report by its identifier.
   * @param reportId the identifier of the report to retrieve
   * @param authentication the authenticated principal
   * @return the report, or 404 when not found or requested by somebody else
   */
  @GetMapping("/{reportId}")
  @Operation(summary = "Get a report by id",
      description = "Retrieves one of the caller's already-generated reports by its identifier.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Report found",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "404", description = "No such report was requested by the caller")
  })
  public ResponseEntity<ReportResource> getReportById(
      @Parameter(description = "Identifier of the report", example = "1", required = true)
      @PathVariable Long reportId,
      Authentication authentication) {
    return reportQueryService.handle(new GetReportByIdQuery(reportId, authentication.getName()))
        .map(ReportResourceFromEntityAssembler::toResourceFromEntity)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   *  Lists every report the authenticated caller has requested, most recent first.
   * @param userId deprecated and ignored; kept so existing clients keep working
   * @param authentication the authenticated principal
   * @return the caller's reports, empty when they have none
   */
  @GetMapping
  @Operation(summary = "List my reports",
      description = "Retrieves every report the authenticated caller has requested, most recent first.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Reports retrieved",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid")
  })
  public ResponseEntity<List<ReportResource>> getReportsByUser(
      @Parameter(description = "Deprecated and ignored: the requester is derived from the access token",
          deprecated = true)
      @RequestParam(required = false) Long userId,
      Authentication authentication) {
    List<Report> reports = reportQueryService.handle(new GetReportsByUserQuery(authentication.getName()));
    List<ReportResource> resources = reports.stream()
        .map(ReportResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(resources);
  }

  /**
   *  Obtains a temporary link to download a report's file.
   * @param reportId the identifier of the report
   * @param userId   deprecated and ignored; kept so existing clients keep working
   * @param authentication the authenticated principal, who must be the report's requester
   * @return the temporary download link, or 404 when it is not available to this user
   */
  @GetMapping("/{reportId}/download")
  @Operation(summary = "Get a temporary download link for a report's file",
      description = "Returns a time-limited URL to download the report's rendered file straight "
          + "from the file storage. Only the user who requested the report can obtain it, and only "
          + "once the report is COMPLETED. A new link must be requested after it expires.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Download link generated",
          content = @Content(schema = @Schema(implementation = ReportDownloadResource.class))),
      @ApiResponse(responseCode = "404",
          description = "Report not found, requested by another user, or without a file")
  })
  public ResponseEntity<ReportDownloadResource> getReportDownloadLink(
      @Parameter(description = "Identifier of the report", example = "1", required = true)
      @PathVariable Long reportId,
      @Parameter(description = "Deprecated and ignored: the requester is derived from the access token",
          deprecated = true)
      @RequestParam(required = false) Long userId,
      Authentication authentication) {
    return reportQueryService.handle(new GetReportDownloadLinkQuery(reportId, authentication.getName()))
        .map(ReportDownloadResourceFromLinkAssembler::toResourceFromLink)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
