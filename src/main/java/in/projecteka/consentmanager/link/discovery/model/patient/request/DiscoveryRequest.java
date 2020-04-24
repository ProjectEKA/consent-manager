package in.projecteka.consentmanager.link.discovery.model.patient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@Getter
public class DiscoveryRequest {
    @NotNull(message = "HIP not specified.")
    private final HIPReference hip;
    private final List<PatientIdentifier> unverifiedIdentifiers;
}
