package in.projecteka.consentmanager.link.link.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Acknowledgement {
    AcknowledgementStatus status;
}