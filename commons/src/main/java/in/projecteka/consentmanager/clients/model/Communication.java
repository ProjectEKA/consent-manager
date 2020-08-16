package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Communication {
    private CommunicationType communicationType;
    private String value;
}
