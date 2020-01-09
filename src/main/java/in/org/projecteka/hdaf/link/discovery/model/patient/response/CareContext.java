package in.org.projecteka.hdaf.link.discovery.model.patient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CareContext {
    private String referenceNumber;
    private String display;
}
