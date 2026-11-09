package pe.edu.upc.ice.track.platform.reporting.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.reporting.application.commandservices.ReportCommandService;
import pe.edu.upc.ice.track.platform.reporting.application.queryservices.ReportQueryService;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.CreateReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform.ReportResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform.RequestReportCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.util.List;

/**
 * REST controller of the Reporting context.
 *
 * <p>Report generation is asynchronous. {@code POST /api/v1/reports} saves the request and answers
 * {@code 202 Accepted} with the report in {@code PENDING} status; the owner then polls
 * {@code GET /api/v1/reports/{id}} until the status is {@code COMPLETED} or {@code FAILED}. A completed
 * report is read with a signed {@code download_url} valid for 15 minutes.</p>
 *
 * <p>Every endpoint is for owners and acts on the authenticated caller. There is no requester
 * parameter anywhere: a report of anybody else answers 404, exactly like a missing one.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reports", description = "Asynchronous report generation and retrieval")
public class ReportsController {

  private static final String REPORT_RESOURCE = "Report";

  private final ReportCommandService reportCommandService;
  private final ReportQueryService reportQueryService;

  public ReportsController(ReportCommandService reportCommandService, ReportQueryService reportQueryService) {
    this.reportCommandService = reportCommandService;
    this.reportQueryService = reportQueryService;
  }

  /**
   * Requests a report. Generation starts in the background.
   *
   * @param resource       the report request
   * @param authentication the authenticated principal, who becomes the requester
   * @return the saved report in {@code PENDING} status, with 202 Accepted
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Request a new report",
      description = "Saves a report request for the authenticated owner in PENDING status and answers at once. "
          + "The report is generated in the background: poll GET /api/v1/reports/{id} until it is COMPLETED "
          + "or FAILED. A site or unit filter must belong to the caller.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Report accepted and queued for generation",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid report request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "The requested site or unit is not reachable by the caller")
  })
  public ResponseEntity<?> requestReport(
      @Valid @RequestBody CreateReportResource resource,
      Authentication authentication) {
    var command = RequestReportCommandFromResourceAssembler.toCommandFromResource(resource, authentication.getName());
    return ResponseEntityAssembler.toResponseEntityFromResult(
        reportCommandService.handle(command),
        report -> ReportResourceFromEntityAssembler.toResourceFromEntity(report, null),
        HttpStatus.ACCEPTED);
  }

  /**
   * Retrieves one of the caller's reports, with its download link once it is completed.
   *
   * @param reportId       the identifier of the report
   * @param authentication the authenticated principal, who must be the requester
   * @return the report, with 200 OK; 404 when it does not exist or belongs to somebody else
   */
  @GetMapping("/{reportId}")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Get a report by id",
      description = "Returns the caller's report with its status. A COMPLETED report also carries a "
          + "download_url, a signed link valid for 15 minutes; a FAILED report carries its failure_reason.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Report found",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND - no such report belongs to the caller")
  })
  public ResponseEntity<?> getReportById(
      @Parameter(description = "Identifier of the report", example = "1", required = true)
      @PathVariable Long reportId,
      Authentication authentication) {
    var username = authentication.getName();
    var report = reportQueryService.handle(new GetReportByIdQuery(reportId, username));
    if (report.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound(REPORT_RESOURCE, String.valueOf(reportId)));
    }
    var downloadUrl = downloadUrlOf(report.get(), username);
    return ResponseEntity.ok(ReportResourceFromEntityAssembler.toResourceFromEntity(report.get(), downloadUrl));
  }

  /**
   * Lists every report the authenticated caller has requested, most recent first.
   *
   * @param authentication the authenticated principal
   * @return the caller's reports, empty when they have none
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "List my reports",
      description = "Retrieves every report the authenticated caller has requested, most recent first.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Reports retrieved",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner")
  })
  public ResponseEntity<List<ReportResource>> getReportsByUser(Authentication authentication) {
    List<Report> reports = reportQueryService.handle(new GetReportsByUserQuery(authentication.getName()));
    List<ReportResource> resources = reports.stream()
        .map(report -> ReportResourceFromEntityAssembler.toResourceFromEntity(report, null))
        .toList();
    return ResponseEntity.ok(resources);
  }

  /**
   * Issues a signed download URL for a completed report that belongs to the caller.
   *
   * @param report   the report
   * @param username the caller
   * @return the URL, or {@code null} when the report is not completed or has no file
   */
  private String downloadUrlOf(Report report, String username) {
    if (report.getStatus() != ReportStatus.COMPLETED) {
      return null;
    }
    return reportQueryService.handle(new GetReportDownloadLinkQuery(report.getId(), username))
        .map(link -> link.url())
        .orElse(null);
  }
}
