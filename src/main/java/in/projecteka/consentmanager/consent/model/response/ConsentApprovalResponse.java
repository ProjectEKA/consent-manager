package in.projecteka.consentmanager.consent.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class ConsentApprovalResponse {
    private List<Consent> consents;
}
