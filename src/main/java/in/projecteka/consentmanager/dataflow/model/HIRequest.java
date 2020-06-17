package in.projecteka.consentmanager.dataflow.model;

import in.projecteka.consentmanager.consent.model.ConsentStatus;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Builder
@Value
public class HIRequest {
    UUID transactionId;
    ConsentStatus sessionStatus;
}