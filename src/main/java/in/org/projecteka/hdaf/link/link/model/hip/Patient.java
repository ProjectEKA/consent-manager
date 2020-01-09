package in.org.projecteka.hdaf.link.link.model.hip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Setter
@EqualsAndHashCode
public class Patient {
    private String consentManagerUserID;
    private String referenceNumber;
    private List<CareContext> careContexts;
}
