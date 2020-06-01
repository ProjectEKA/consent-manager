package in.projecteka.consentmanager.link.discovery.model.patient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@Data
public class GatewayResponse {
    private String requestId;
}
