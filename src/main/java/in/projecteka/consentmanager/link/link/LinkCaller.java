package in.projecteka.consentmanager.link.link;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LinkCaller {
    String hipId;
    String healthId;
    String sessionId;
}