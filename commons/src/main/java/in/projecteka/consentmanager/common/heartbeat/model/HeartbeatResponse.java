package in.projecteka.consentmanager.common.heartbeat.model;

import in.projecteka.consentmanager.clients.model.Error;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Builder
@Value
public class HeartbeatResponse {
    private LocalDateTime timeStamp;
    private Status status;
    private Error error;
}
