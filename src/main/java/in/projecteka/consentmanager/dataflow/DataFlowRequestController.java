package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class DataFlowRequestController {
    private final DataFlowRequester dataFlowRequester;

    @PostMapping("/health-information/request")
    public Mono<DataFlowRequestResponse> requestHealthInformation(
            @RequestBody in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester -> dataFlowRequester.requestHealthData(requester.getUserName(), dataFlowRequest));
    }
}
