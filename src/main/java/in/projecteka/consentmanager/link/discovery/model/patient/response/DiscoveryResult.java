package in.projecteka.consentmanager.link.discovery.model.patient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.consentmanager.clients.model.Error;
import lombok.Builder;
import lombok.Value;
import java.util.UUID;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoveryResult {
    private UUID requestId;
    private UUID transactionId;
    private Patient patient;
    private Error error;
}
