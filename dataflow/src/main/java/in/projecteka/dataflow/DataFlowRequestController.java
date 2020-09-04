package in.projecteka.dataflow;

import in.projecteka.dataflow.model.GatewayDataFlowRequest;
import in.projecteka.dataflow.model.HealthInfoNotificationRequest;
import in.projecteka.dataflow.model.HealthInformationResponse;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_ON_REQUEST;
import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_REQUEST;
import static in.projecteka.library.clients.model.ClientError.tooManyRequests;
import static in.projecteka.library.common.Constants.CORRELATION_ID;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@RestController
@AllArgsConstructor
public class DataFlowRequestController {
    private final DataFlowRequester dataFlowRequester;
    private final RequestValidator validator;

    @PostMapping(PATH_HEALTH_INFORMATION_REQUEST)
    @ResponseStatus(ACCEPTED)
    public Mono<Void> requestHealthInformationV1(@Valid @RequestBody GatewayDataFlowRequest dataFlowRequest) {
        return just(dataFlowRequest)
                .filterWhen(req -> validator.validate(req.getRequestId().toString(), req.getTimestamp()))
                .switchIfEmpty(error(tooManyRequests()))
                .flatMap(req -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                        .doOnSuccess(requester -> defer(() ->
                                validator.put(req.getRequestId().toString(), req.getTimestamp())
                                        .then(dataFlowRequester.requestHealthDataInfo(dataFlowRequest)))
                                .subscriberContext(ctx -> {
                                    Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                                    return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                                            .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                                }).subscribe())
                        .then());
    }

    @PostMapping(PATH_HEALTH_INFORMATION_ON_REQUEST)
    @ResponseStatus(ACCEPTED)
    public Mono<Void> onRequestHealthInformationV1(
            @RequestBody @Valid HealthInformationResponse healthInformationResponse) {
        return just(healthInformationResponse)
                .filterWhen(res -> validator.validate(res.getRequestId().toString(), res.getTimestamp()))
                .switchIfEmpty(error(tooManyRequests()))
                .flatMap(res -> validator.put(
                        healthInformationResponse.getRequestId().toString(),
                        healthInformationResponse.getTimestamp())
                        .then(dataFlowRequester.updateDataflowRequestStatus(healthInformationResponse)));
    }

    @PostMapping(PATH_HEALTH_INFORMATION_NOTIFY)
    @ResponseStatus(ACCEPTED)
    public Mono<Void> healthInformationNotify(@RequestBody HealthInfoNotificationRequest notificationRequest) {
        return just(notificationRequest)
                .filterWhen(req -> validator.validate(req.getRequestId().toString(), req.getTimestamp()))
                .switchIfEmpty(error(tooManyRequests()))
                .flatMap(req -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> (ServiceCaller) securityContext.getAuthentication().getPrincipal())
                        .doOnSuccess(requester -> defer(() ->
                                validator.put(req.getRequestId().toString(), req.getTimestamp())
                                        .then(dataFlowRequester.notifyHealthInformationStatus(notificationRequest)))
                                .subscriberContext(ctx -> {
                                    Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                                    return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                                            .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                                }).subscribe())
                        .then());
    }
}
