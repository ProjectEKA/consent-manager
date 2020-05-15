package in.projecteka.consentmanager.link.discovery.model.patient.response;

import in.projecteka.consentmanager.clients.model.Error;
import lombok.Builder;
import lombok.Value;
import java.util.UUID;

@Value
@Builder
public class DiscoveryResult {
    private UUID requestId;
    private UUID transactionId;
    private Patient patient;
    private Error error;
}
