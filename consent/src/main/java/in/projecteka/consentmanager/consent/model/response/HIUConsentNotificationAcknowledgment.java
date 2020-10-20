package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.library.clients.model.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Value
public class HIUConsentNotificationAcknowledgment {
    UUID requestId;
    LocalDateTime timestamp;
    List<ConsentNotificationResponse> acknowledgement;
    RespError error;
    GatewayResponse resp;

    @Value
    public static class ConsentNotificationResponse {
        Status status;
        String consentId;
    }

    enum Status {
        OK
    }
}
