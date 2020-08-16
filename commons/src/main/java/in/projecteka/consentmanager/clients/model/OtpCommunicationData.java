package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpCommunicationData {
    private String mode;
    private String value;
}
