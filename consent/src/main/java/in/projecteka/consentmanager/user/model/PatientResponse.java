package in.projecteka.consentmanager.user.model;

import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class PatientResponse {
    UUID requestId;
    LocalDateTime timestamp;
    Patient patient;
    RespError error;
    @NotNull
    GatewayResponse resp;
}

