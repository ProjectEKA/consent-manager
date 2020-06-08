package in.projecteka.consentmanager.user.model;

import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
@Builder
public class PatientResponse {
    private UUID requestId;
    private String timestamp;
    private Patient patient;
    private RespError error;
    @NotNull
    private GatewayResponse resp;
}

