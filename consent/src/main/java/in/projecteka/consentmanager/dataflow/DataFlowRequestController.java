package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.common.ServiceCaller;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import in.projecteka.consentmanager.dataflow.model.GatewayDataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInformationResponse;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.RequestValidator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_ON_REQUEST;
import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_REQUEST;

@RestController
@AllArgsConstructor
public class DataFlowRequestController {
    private final DataFlowRequester dataFlowRequester;
    private final RequestValidator validator;

    @Deprecated
    @PostMapping("/health-information/request")
    public Mono<DataFlowRequestResponse> requestHealthInformation(@RequestBody DataFlowRequest dataFlowRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                .flatMap(requester -> dataFlowRequester.requestHealthData(dataFlowRequest));
    }

    @PostMapping(PATH_HEALTH_INFORMATION_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> requestHealthInformationV1(@Valid @RequestBody GatewayDataFlowRequest dataFlowRequest) {
        return Mono.just(dataFlowRequest)
                .filterWhen(req -> validator.validate(req.getRequestId().toString(), req.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(req -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                        .doOnSuccess(requester -> Mono.defer(() ->
                                validator.put(req.getRequestId().toString(), req.getTimestamp())
                                        .then(dataFlowRequester.requestHealthDataInfo(dataFlowRequest))).subscribe())
                        .then());
    }

    @PostMapping(PATH_HEALTH_INFORMATION_ON_REQUEST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> onRequestHealthInformationV1(
            @RequestBody @Valid HealthInformationResponse healthInformationResponse) {
        return Mono.just(healthInformationResponse)
                .filterWhen(res -> validator.validate(res.getRequestId().toString(), res.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(res -> validator.put(
                        healthInformationResponse.getRequestId().toString(),
                        healthInformationResponse.getTimestamp())
                        .then(dataFlowRequester.updateDataflowRequestStatus(healthInformationResponse)));
    }

    @PostMapping(PATH_HEALTH_INFORMATION_NOTIFY)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> healthInformationNotify(@RequestBody HealthInfoNotificationRequest notificationRequest) {
        return Mono.just(notificationRequest)
                .filterWhen(req -> validator.validate(req.getRequestId().toString(), req.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(req -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                        .doOnSuccess(requester -> Mono.defer(() ->
                                validator.put(req.getRequestId().toString(), req.getTimestamp())
                                        .then(dataFlowRequester.notifyHealthInformationStatus(notificationRequest)))
                                .subscribe())
                        .then());
    }
}
