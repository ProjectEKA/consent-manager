package in.projecteka.consentmanager.common.heartbeat.model;

import in.projecteka.consentmanager.clients.model.Error;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class HeartbeatResponse {
    private String timeStamp;
    private Status status;
    private Error error;
}
