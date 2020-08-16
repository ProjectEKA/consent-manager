package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class ConsentRequest {
    private UUID id;
    private RequestedDetail detail;
}
