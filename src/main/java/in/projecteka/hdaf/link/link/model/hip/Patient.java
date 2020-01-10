package in.projecteka.hdaf.link.link.model.hip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class Patient {
    private String consentManagerUserID;
    private String referenceNumber;
    private List<CareContext> careContexts;
}
