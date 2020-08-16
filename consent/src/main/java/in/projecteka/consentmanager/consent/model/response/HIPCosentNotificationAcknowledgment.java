package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Value
public class HIPCosentNotificationAcknowledgment {
    UUID requestId;
    LocalDateTime timestamp;
    ConsentNotificationResponse acknowledgement;
    RespError error;
    GatewayResponse resp;

    @Value
    public static class ConsentNotificationResponse {
        Status status;
        String consentId;
    }

    enum  Status {
        OK
    }
}
