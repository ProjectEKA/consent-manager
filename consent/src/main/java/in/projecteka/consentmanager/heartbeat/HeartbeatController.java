package in.projecteka.consentmanager.heartbeat;

import in.projecteka.library.common.Constants;
import in.projecteka.library.common.heartbeat.Heartbeat;
import in.projecteka.library.common.heartbeat.model.HeartbeatResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.library.common.heartbeat.model.Status.UP;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@AllArgsConstructor
public class HeartbeatController {
    private final Heartbeat heartbeat;

    @GetMapping(Constants.PATH_READINESS)
    public Mono<ResponseEntity<HeartbeatResponse>> getReadiness() {
        return heartbeat.getStatus()
                .map(heartbeatResponse ->
                        heartbeatResponse.getStatus() == UP
                                ? new ResponseEntity<>(heartbeatResponse, OK)
                                : new ResponseEntity<>(heartbeatResponse, SERVICE_UNAVAILABLE));
    }

    @GetMapping(Constants.PATH_HEARTBEAT)
    public Mono<ResponseEntity<HeartbeatResponse>> getLiveliness() {
        HeartbeatResponse heartbeatResponse = HeartbeatResponse.builder()
                .status(UP)
                .timeStamp(now(UTC))
                .build();
        return Mono.just(new ResponseEntity<>(heartbeatResponse, OK));
    }
}
