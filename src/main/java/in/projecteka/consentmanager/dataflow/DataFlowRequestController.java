package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class DataFlowRequestController {
    private final DataFlowRequester dataFlowRequester;
    private final Authenticator authenticator;

    @PostMapping("/health-information/request")
    public Mono<DataFlowRequestResponse> requestHealthInformation(
            @RequestHeader(value = "Authorization") String authorization,
            @RequestBody in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest) {
        return authenticator.userFrom(authorization)
                .map(Caller::getUserName)
                .flatMap(hiu -> dataFlowRequester.requestHealthData(hiu, dataFlowRequest));
    }
}
