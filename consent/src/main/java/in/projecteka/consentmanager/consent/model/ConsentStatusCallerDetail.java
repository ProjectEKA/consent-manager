package in.projecteka.consentmanager.consent.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConsentStatusCallerDetail {
    ConsentStatus status;
    String hiuId;
}
