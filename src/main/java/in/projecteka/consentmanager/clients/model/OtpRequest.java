package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpRequest {
    private String sessionId;
    private OtpCommunicationData communication;
    private OtpCreationDetail creationDetail;
}
