package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;

/**
 * Resource received to replace the editable details of a technician.
 *
 * <p>A full replacement: every mandatory field must be supplied. The speciality and the
 * certification number are checked against the same formats the domain enforces, so a malformed
 * value is rejected before reaching the aggregate. The email address is not editable here - it
 * belongs to the platform account.</p>
 */
@Schema(
    name = "UpdateTechnicianRequest",
    description = "Technician update request",
    example = "{\"fullName\": \"Jane Doe\", \"phone\": \"+51 987654321\", \"street\": \"Av. Primavera\", \"number\": \"123\", \"city\": \"Lima\", \"postalCode\": \"15023\", \"country\": \"Peru\", \"speciality\": \"Refrigeration\", \"certificationNumber\": \"CERT-2024-001\"}"
)
public record UpdateTechnicianResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Technician full name", example = "Jane Doe")
    String fullName,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Phone number, optionally prefixed by the country code and a space", example = "+51 987654321")
    String phone,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Street of the address", example = "Av. Primavera")
    String street,

    @Schema(description = "Street number or apartment", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String number,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "City of the address", example = "Lima")
    String city,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Postal code of the address", example = "15023")
    String postalCode,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Country of the address", example = "Peru")
    String country,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = Speciality.MAX_LENGTH, message = "{validation.size.max}")
    @Schema(description = "Technician speciality", example = "Refrigeration", maxLength = Speciality.MAX_LENGTH)
    String speciality,

    @NotBlank(message = "{validation.not-blank}")
    @Pattern(regexp = TechnicianProfile.CERTIFICATION_NUMBER_PATTERN, message = "{validation.certification-number}")
    @Schema(description = "Technician certification number: 3 to 50 letters, digits, '-', '.' or '/'",
        example = "CERT-2024-001", pattern = TechnicianProfile.CERTIFICATION_NUMBER_PATTERN)
    String certificationNumber
) {
}
