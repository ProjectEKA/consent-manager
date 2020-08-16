package in.projecteka.consentmanager.clients.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Acknowledgement {
    AcknowledgementStatus status;
}