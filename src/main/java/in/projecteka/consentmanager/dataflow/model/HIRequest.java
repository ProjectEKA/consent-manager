package in.projecteka.consentmanager.dataflow.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Builder
@Value
public class HIRequest {
    UUID transactionId;
    RequestStatus sessionStatus;
}