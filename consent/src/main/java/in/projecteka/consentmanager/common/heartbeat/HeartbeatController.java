package in.projecteka.consentmanager.common.heartbeat;

import in.projecteka.library.common.Constants;
import in.projecteka.library.common.heartbeat.Heartbeat;
import in.projecteka.library.common.heartbeat.model.HeartbeatResponse;
import in.projecteka.library.common.heartbeat.model.Status;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@AllArgsConstructor
public class HeartbeatController {
    private final Heartbeat heartbeat;

    @GetMapping(Constants.PATH_HEARTBEAT)
    public Mono<ResponseEntity<HeartbeatResponse>> getProvidersByName() {
        return heartbeat.getStatus()
                .map(heartbeatResponse ->
                        heartbeatResponse.getStatus() == Status.UP
                        ? new ResponseEntity<>(heartbeatResponse, OK)
                        : new ResponseEntity<>(heartbeatResponse, SERVICE_UNAVAILABLE));
    }
}
