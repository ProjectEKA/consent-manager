package in.org.projecteka.hdaf.link.discovery.model.patient.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscoveryResponse {
    private Patient patient;
    private String transactionId;
}
