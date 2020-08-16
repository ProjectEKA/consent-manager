package in.projecteka.consentmanager.link.discovery.model.patient.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class DiscoveryResponse {
    Patient patient;
    UUID transactionId;
}
