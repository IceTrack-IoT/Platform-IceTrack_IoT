package pe.edu.upc.ice.track.platform.service.request.interfaces.rest;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ReviewCommandService;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ReviewResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.SubmitReviewResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.UpdateReviewResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.ReviewResourceFromEntityAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.SubmitReviewCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform.UpdateReviewCommandFromResourceAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;

/**
 * REST controller through which an owner reviews the technician who completed one of its service
 * requests.
 *
 * <p>Both endpoints are {@code OWNER_ROLE} only. A request can be reviewed once, after it is
 * completed, and its review edited for 48 hours; outside those rules the answer is
 * {@code 409 REVIEW_NOT_ALLOWED}, or {@code 409 DUPLICATE_RESOURCE_CONFLICT} for a second review.
 * Reading a review goes through {@code GET /api/v1/service-requests/{id}/review}.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reviews", description = "Service request review endpoints: submission and edition within the 48-hour window")
public class ReviewsController {

  private final ReviewCommandService reviewCommandService;

  /**
   * Constructor
   *
   * @param reviewCommandService The {@link ReviewCommandService} instance
   */
  public ReviewsController(ReviewCommandService reviewCommandService) {
    this.reviewCommandService = reviewCommandService;
  }

  /**
   * Reviews one of the authenticated owner's completed service requests.
   *
   * @param resource       The {@link SubmitReviewResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The submitted {@link ReviewResource}, with 201 Created
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  @Operation(
      summary = "Submit a review",
      description = "Reviews the technician who completed one of the authenticated owner's requests. "
          + "The review can be edited during the following 48 hours.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Review submitted",
          content = @Content(schema = @Schema(implementation = ReviewResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a score outside 1 to 5"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such service request is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "REVIEW_NOT_ALLOWED for a request not completed, or DUPLICATE_RESOURCE_CONFLICT for a request already reviewed")
  })
  public ResponseEntity<?> submitReview(
      @Valid @RequestBody SubmitReviewResource resource,
      Authentication authentication) {
    var command = SubmitReviewCommandFromResourceAssembler.toCommandFromResource(authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        reviewCommandService.handle(command),
        ReviewResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.CREATED);
  }

  /**
   * Edits one of the authenticated owner's reviews within its edit window.
   *
   * @param reviewId       The review identifier
   * @param resource       The {@link UpdateReviewResource} payload
   * @param authentication The authenticated principal, whose name identifies the caller
   * @return The updated {@link ReviewResource}
   */
  @PutMapping(value = "/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('OWNER_ROLE')")
  @Operation(
      summary = "Update a review",
      description = "Replaces the scores and the comment of one of the authenticated owner's reviews, "
          + "within 48 hours of its submission.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Review updated",
          content = @Content(schema = @Schema(implementation = ReviewResource.class))),
      @ApiResponse(responseCode = "400", description = "Invalid input data, such as a score outside 1 to 5"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required or invalid"),
      @ApiResponse(responseCode = "403", description = "Forbidden - the authenticated account is not an owner"),
      @ApiResponse(responseCode = "404", description = "No such review is reachable by the caller"),
      @ApiResponse(responseCode = "409", description = "REVIEW_NOT_ALLOWED - the edit window has closed")
  })
  public ResponseEntity<?> updateReview(
      @PathVariable
      @Parameter(description = "Review unique identifier", example = "5", required = true)
      Long reviewId,
      @Valid @RequestBody UpdateReviewResource resource,
      Authentication authentication) {
    var command = UpdateReviewCommandFromResourceAssembler.toCommandFromResource(
        reviewId, authentication.getName(), resource);
    return ResponseEntityAssembler.toResponseEntityFromResult(
        reviewCommandService.handle(command),
        ReviewResourceFromEntityAssembler::toResourceFromEntity,
        HttpStatus.OK);
  }
}
