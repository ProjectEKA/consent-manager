package in.projecteka.consentmanager.link.link.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewCCLinkEvent implements Serializable {
    private String hipId;
    private String healthNumber;
    private LocalDateTime timestamp;
    private List<PatientCareContext> careContexts;
}
