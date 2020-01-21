package in.projecteka.consentmanager.consent.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentRequestsRepresentation {
    private int size;
    private int limit;
    private int offset;
    private List<ConsentRequestDetail> requests;
}
