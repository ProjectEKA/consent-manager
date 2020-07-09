package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.RequestValidator;
import in.projecteka.consentmanager.common.ServiceCaller;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import in.projecteka.consentmanager.dataflow.model.GatewayDataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInformationResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_NOTIFY;
import javax.validation.Valid;

import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_ON_REQUEST;
import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_REQUEST;

@RestController
@AllArgsConstructor
public class DataFlowRequestController {
    private final DataFlowRequester dataFlowRequester;
    private final RequestValidator validator;
    private final CacheAdapter<String, String> cacheForReplayAttack;

    @PostMapping("/health-information/request")
    public Mono<DataFlowRequestResponse> requestHealthInformation(@RequestBody DataFlowRequest dataFlowRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester -> dataFlowRequester.requestHealthData(dataFlowRequest));
    }

    @PostMapping(V_1_HEALTH_INFORMATION_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> requestHealthInformationV1(@Valid @RequestBody GatewayDataFlowRequest dataFlowRequest) {
        return Mono.just(dataFlowRequest)
                .filterWhen(req -> validator.validate(req.getRequestId().toString(), req.getTimestamp().toString()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(req -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                        .doOnSuccess(requester -> Mono.defer(() ->
                                cacheForReplayAttack.put(req.getRequestId().toString(), req.getTimestamp().toString())
                                        .then(dataFlowRequester.requestHealthDataInfo(dataFlowRequest))
                        ).subscribe())
                        .then());
    }

    @PostMapping(V_1_HEALTH_INFORMATION_ON_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> onRequestHealthInformationV1(@RequestBody @Valid HealthInformationResponse healthInformationResponse) {
        return Mono.just(healthInformationResponse)
                .filterWhen(res -> validator.validate(res.getRequestId().toString(), res.getTimestamp().toString()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(res -> cacheForReplayAttack.put(
                        healthInformationResponse.getRequestId().toString(),
                        healthInformationResponse.getTimestamp().toString())
                        .then(dataFlowRequester.updateDataflowRequestStatus(healthInformationResponse))
                );
    }



    @PostMapping(V_1_HEALTH_INFORMATION_NOTIFY)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> healthInformationNotify(@RequestBody HealthInfoNotificationRequest notificationRequest) {
        return Mono.just(notificationRequest)
                .filterWhen(req -> validator.validate(req.getRequestId().toString(), req.getTimestamp().toString()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(req -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                        .doOnSuccess(requester -> Mono.defer(() ->
                                cacheForReplayAttack.put(req.getRequestId().toString(), req.getTimestamp().toString())
                                        .then(dataFlowRequester.notifyHealthInformationStatus(notificationRequest))
                        ).subscribe())
                        .then());
    }
}
