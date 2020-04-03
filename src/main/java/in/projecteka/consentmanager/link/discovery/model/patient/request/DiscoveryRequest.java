package in.projecteka.consentmanager.link.discovery.model.patient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiscoveryRequest {
    @NotNull(message = "HIP not specified.")
    private HIPReference hip;
    private List<PatientIdentifier> unverifiedIdentifiers;
}
