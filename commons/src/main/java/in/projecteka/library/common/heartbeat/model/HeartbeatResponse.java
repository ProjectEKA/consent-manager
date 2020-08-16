package in.projecteka.library.common.heartbeat.model;

import in.projecteka.library.clients.model.Error;
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
