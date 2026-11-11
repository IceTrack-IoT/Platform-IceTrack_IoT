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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.reporting.application.commandservices.ReportCommandService;
import pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices.KpiPayloadReader;
import pe.edu.upc.ice.track.platform.reporting.application.queryservices.ReportQueryService;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.DeleteReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportPageByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.CreateReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.PagedReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportDownloadLinkResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportStatusResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources.ReportTypeResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform.ReportResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.reporting.interfaces.rest.transform.RequestReportCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST controller of the Reporting context.
 *
 * <p>Report generation is asynchronous. {@code POST /api/v1/reports} saves the request and answers
 * {@code 202 Accepted} with the report in {@code PENDING} status; the owner then polls
 * {@code GET /api/v1/reports/{id}} until the status is {@code COMPLETED} or {@code FAILED}. A completed
 * report is read with a signed {@code download_url} valid for 15 minutes and its KPIs as a JSON array;
 * {@code GET /api/v1/reports/{id}/download-link} issues a link on its own, with its exact expiry and
 * the file details.</p>
 *
 * <p>The history is listed as a plain array, or as the paged envelope when both {@code page} and
 * {@code size} are sent; either way it can be filtered by name, type, status and request date. Paged
 * items are always summaries, without {@code result_payload} or {@code kpis}; a plain listing returns
 * summaries when asked with {@code include=summary}.</p>
 *
 * <p>A finished report ({@code COMPLETED} or {@code FAILED}) can be deleted with
 * {@code DELETE /api/v1/reports/{id}}, which also removes its stored file.</p>
 *
 * <p>Every endpoint is for owners and acts on the authenticated caller. There is no requester
 * parameter anywhere: a report of anybody else answers 404, exactly like a missing one.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reports", description = "Asynchronous report generation and retrieval")
public class ReportsController {

  private static final String INCLUDE_SUMMARY = "summary";

  private final ReportCommandService reportCommandService;
  private final ReportQueryService reportQueryService;
  private final KpiPayloadReader kpiPayloadReader;

  public ReportsController(
      ReportCommandService reportCommandService,
      ReportQueryService reportQueryService,
      KpiPayloadReader kpiPayloadReader) {
    this.reportCommandService = reportCommandService;
    this.reportQueryService = reportQueryService;
    this.kpiPayloadReader = kpiPayloadReader;
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
          + "or FAILED. A site or unit filter must belong to the caller. A SERVICE_INTERVENTION report documents "
          + "one COMPLETED service request of the caller, named by filters.service_request_id, and may leave "
          + "from and to out to cover the request's own period; every other type requires from and to and "
          + "refuses service_request_id.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Report accepted and queued for generation",
          content = @Content(schema = @Schema(implementation = ReportResource.class))),
      @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR - invalid report request, such as a missing "
          + "period, or service_request_id missing for SERVICE_INTERVENTION or sent for another type"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner"),
      @ApiResponse(responseCode = "404", description = "SITE_NOT_FOUND, EQUIPMENT_NOT_FOUND or "
          + "SERVICE_REQUEST_NOT_FOUND - a filter is not reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION - the service request of a "
          + "SERVICE_INTERVENTION report is not COMPLETED")
  })
  public ResponseEntity<?> requestReport(
      @Valid @RequestBody CreateReportResource resource,
      Authentication authentication) {
    var command = RequestReportCommandFromResourceAssembler.toCommandFromResource(resource, authentication.getName());
    return ResponseEntityAssembler.toResponseEntityFromResult(
        reportCommandService.handle(command),
        report -> ReportResourceFromEntityAssembler.toResourceFromEntity(report, null, null),
        HttpStatus.ACCEPTED);
  }

  /**
   * Retrieves one of the caller's reports, with its download link and KPIs once it is completed.
   *
   * @param reportId       the identifier of the report
   * @param authentication the authenticated principal, who must be the requester
   * @return the report, with 200 OK; 404 when it does not exist or belongs to somebody else
   */
  @GetMapping("/{reportId}")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Get a report by id",
      description = "Returns the caller's report with its status. A COMPLETED report also carries a "
          + "download_url, a signed link valid for 15 minutes, and its KPIs as the kpis array; a FAILED report "
          + "carries its failure_reason.",
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
          ApplicationError.notFound("Report", String.valueOf(reportId)));
    }
    var downloadUrl = downloadUrlOf(report.get(), username);
    var kpis = kpiPayloadReader.read(report.get()).orElse(null);
    return ResponseEntity.ok(ReportResourceFromEntityAssembler.toResourceFromEntity(report.get(), downloadUrl, kpis));
  }

  /**
   * Issues a signed download link for one of the caller's completed reports.
   *
   * @param reportId       the identifier of the report
   * @param authentication the authenticated principal, who must be the requester
   * @return the link with its expiry and file details, never cached
   */
  @GetMapping("/{reportId}/download-link")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Get a download link for a report",
      description = "Signs a link to download the file of the caller's COMPLETED report, valid for 15 minutes. "
          + "expires_at is the exact instant the signature expires. The link asks the browser to save the file "
          + "as file_name instead of opening it. The answer carries Cache-Control: no-store.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Download link issued",
          content = @Content(schema = @Schema(implementation = ReportDownloadLinkResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "ACCESS_DENIED - the caller is not an owner",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND - no such report belongs to the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION - the report is not COMPLETED; "
          + "details names its status",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getReportDownloadLink(
      @Parameter(description = "Identifier of the report", example = "8", required = true)
      @PathVariable Long reportId,
      Authentication authentication) {
    Result<ReportDownloadLink, ApplicationError> link =
        reportQueryService.handle(new GetReportDownloadLinkQuery(reportId, authentication.getName()));
    return switch (link) {
      case Result.Success<ReportDownloadLink, ApplicationError> success -> ResponseEntity.ok()
          .cacheControl(CacheControl.noStore())
          .body(ReportResourceFromEntityAssembler.toResourceFromDownloadLink(success.value()));
      case Result.Failure<ReportDownloadLink, ApplicationError> failure ->
          ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
    };
  }

  /**
   * Deletes one of the caller's finished reports, together with its stored file.
   *
   * @param reportId       the identifier of the report
   * @param authentication the authenticated principal, who must be the requester
   * @return 204 No Content, or the reason the report cannot be deleted
   */
  @DeleteMapping("/{reportId}")
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "Delete a report",
      description = "Deletes the caller's COMPLETED or FAILED report and removes its stored file; a download link "
          + "issued before stops working once the file is gone. A report still PENDING or GENERATING belongs to "
          + "the generation worker and cannot be deleted. Deleting the same report again answers 404.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Report and file deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "ACCESS_DENIED - the caller is not an owner",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND - no such report belongs to the caller",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION - the report is PENDING or "
          + "GENERATING; details names its status",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> deleteReport(
      @Parameter(description = "Identifier of the report", example = "8", required = true)
      @PathVariable Long reportId,
      Authentication authentication) {
    return switch (reportCommandService.handle(new DeleteReportCommand(reportId, authentication.getName()))) {
      case Result.Success<Long, ApplicationError> success -> ResponseEntity.noContent().build();
      case Result.Failure<Long, ApplicationError> failure ->
          ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
    };
  }

  /**
   * Lists the reports the authenticated caller has requested, most recent first.
   *
   * @param search         case-insensitive fragment of the report name; optional
   * @param type           restrict to these report types; optional
   * @param status         restrict to these statuses; optional
   * @param requestedFrom  inclusive lower bound of the request instant; optional
   * @param requestedTo    inclusive upper bound of the request instant; optional
   * @param include        {@code summary} to omit the payload and KPIs from a plain listing; optional
   * @param page           zero-based page index; with {@code size}, switches to the paged envelope
   * @param size           page size, capped at 100; with {@code page}, switches to the paged envelope
   * @param authentication the authenticated principal
   * @return the caller's matching reports, as an array or as one page
   */
  @GetMapping
  @PreAuthorize("hasAuthority('" + RoleAuthorities.ROLE_OWNER + "')")
  @Operation(summary = "List my reports",
      description = "Lists the reports the authenticated caller has requested, most recent first, optionally "
          + "filtered by name, type, status and request date; every filter combines with the others. Without "
          + "paging the answer is a plain array of reports; include=summary leaves result_payload and kpis out of "
          + "each item. When both page and size are sent the answer is the paged envelope "
          + "(PagedReportResponse), whose items are always summaries. size is capped at 100. No item carries a "
          + "download_url: read the report by id, or ask for its download link.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200",
          description = "Reports retrieved: an array of ReportResponse, or a PagedReportResponse when page and "
              + "size are both sent",
          content = @Content(schema = @Schema(oneOf = {ReportResource[].class, PagedReportResource.class}))),
      @ApiResponse(responseCode = "400",
          description = "VALIDATION_ERROR - unknown type, status or include value, search longer than 150 "
              + "characters, requested_from after requested_to, negative page or size below 1",
          content = @Content(schema = @Schema(implementation = ErrorResource.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the caller is not an owner")
  })
  public ResponseEntity<?> getReportsByUser(
      @RequestParam(required = false)
      @Parameter(description = "Case-insensitive fragment of the report name, at most 150 characters",
          example = "october")
      String search,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these report types, comma-separated", example = "TEMPERATURE_EXCURSION")
      List<ReportTypeResource> type,
      @RequestParam(required = false)
      @Parameter(description = "Restrict to these statuses, comma-separated", example = "COMPLETED,FAILED")
      List<ReportStatusResource> status,
      @RequestParam(name = "requested_from", required = false)
      @Parameter(description = "Inclusive lower bound of requested_at, ISO 8601 with Z or an offset",
          example = "2026-10-01T00:00:00Z")
      Instant requestedFrom,
      @RequestParam(name = "requested_to", required = false)
      @Parameter(description = "Inclusive upper bound of requested_at, ISO 8601 with Z or an offset",
          example = "2026-10-31T23:59:59Z")
      Instant requestedTo,
      @RequestParam(required = false)
      @Parameter(description = "Only accepts 'summary', to leave result_payload and kpis out of a plain listing",
          example = "summary")
      String include,
      @RequestParam(required = false)
      @Parameter(description = "Zero-based page index; sent with size, the answer is paged", example = "0")
      Integer page,
      @RequestParam(required = false)
      @Parameter(description = "Page size, capped at 100; sent with page, the answer is paged", example = "20")
      Integer size,
      Authentication authentication) {
    var summary = isSummary(include);
    var filter = new ReportListFilter(
        search,
        toDomainSet(type, ReportTypeResource::toDomain),
        toDomainSet(status, ReportStatusResource::toDomain),
        requestedFrom,
        requestedTo);
    if (page != null && size != null) {
      var reportPage = reportQueryService.handle(new GetReportPageByUserQuery(authentication.getName(), filter, page, size));
      return ResponseEntity.ok(ReportResourceFromEntityAssembler.toPagedResourceFromPage(reportPage));
    }
    // Paging values sent alone do not switch to the envelope, but they are still checked.
    if (page != null && page < 0) {
      throw new IllegalArgumentException("Parameter 'page' must not be negative");
    }
    if (size != null && size < 1) {
      throw new IllegalArgumentException("Parameter 'size' must be at least 1");
    }
    List<Report> reports = reportQueryService.handle(new GetReportsByUserQuery(authentication.getName(), filter));
    List<ReportResource> resources = reports.stream()
        .map(report -> summary
            ? ReportResourceFromEntityAssembler.toSummaryResourceFromEntity(report)
            : ReportResourceFromEntityAssembler.toResourceFromEntity(report, null, kpiPayloadReader.read(report).orElse(null)))
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
        .toOptional()
        .map(ReportDownloadLink::url)
        .orElse(null);
  }

  /**
   * Reads the {@code include} parameter, which only accepts {@code summary}.
   *
   * @throws IllegalArgumentException for any other value, answered with 400 by
   *                                  {@code GlobalExceptionHandler}
   */
  private static boolean isSummary(String include) {
    if (include == null || include.isBlank()) {
      return false;
    }
    if (INCLUDE_SUMMARY.equalsIgnoreCase(include.strip())) {
      return true;
    }
    throw new IllegalArgumentException("Parameter 'include' only accepts 'summary'");
  }

  /**
   * Translates a multi-value enum parameter into its domain counterpart. An empty entry, as left by a
   * trailing comma, is ignored rather than failing the whole request.
   */
  private static <R, D> Set<D> toDomainSet(List<R> values, Function<R, D> toDomain) {
    if (values == null) {
      return Set.of();
    }
    return values.stream().filter(Objects::nonNull).map(toDomain).collect(Collectors.toUnmodifiableSet());
  }
}
