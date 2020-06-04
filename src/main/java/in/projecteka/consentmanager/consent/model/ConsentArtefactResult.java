package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ConsentArtefactResult {
    private UUID requestId;
    private String timestamp;
    private Consent consent;
    private RespError error;
    @NotNull
    private GatewayResponse resp;
}
