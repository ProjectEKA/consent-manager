package in.projecteka.consentmanager.link.discovery.model.patient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class DiscoveryResponse {
    private final Patient patient;
    private final String transactionId;
}
