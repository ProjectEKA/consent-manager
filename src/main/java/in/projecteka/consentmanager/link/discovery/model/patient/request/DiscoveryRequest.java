package in.projecteka.consentmanager.link.discovery.model.patient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Value
public class DiscoveryRequest {
    private UUID requestId;
    @NotNull(message = "HIP not specified.") HIPReference hip;
    List<PatientIdentifier> unverifiedIdentifiers;
}
