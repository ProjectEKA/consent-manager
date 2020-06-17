package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.common.ServiceCaller;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import in.projecteka.consentmanager.dataflow.model.GatewayDataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_REQUEST;

@RestController
@AllArgsConstructor
public class DataFlowRequestController {
    private final DataFlowRequester dataFlowRequester;

    @PostMapping("/health-information/request")
    public Mono<DataFlowRequestResponse> requestHealthInformation(@RequestBody DataFlowRequest dataFlowRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester -> dataFlowRequester.requestHealthData(requester.getClientId(), dataFlowRequest));
    }

    @PostMapping("/health-information/notification")
    public Mono<Void> notify(@RequestBody HealthInfoNotificationRequest notificationRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester ->
                        dataFlowRequester.notifyHealthInfoStatus(requester.getClientId(), notificationRequest));
    }

    @PostMapping(V_1_HEALTH_INFORMATION_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> requestHealthInformationV1(@Valid @RequestBody GatewayDataFlowRequest dataFlowRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .doOnSuccess(requester -> Mono.defer(() -> dataFlowRequester.requestHealthDataInfo(dataFlowRequest)).subscribe())
                .then();
    }
}
