package in.projecteka.consentmanager.clients.model;

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
