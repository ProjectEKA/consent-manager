package in.projecteka.consentmanager.userauth;

import in.projecteka.consentmanager.userauth.model.AuthInitRequest;
import in.projecteka.consentmanager.userauth.model.UserAuthConfirmRequest;
import in.projecteka.consentmanager.userauth.model.FetchAuthModesRequest;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.RequestValidator;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.userauth.Constants.PATH_USER_FETCH_AUTH_MODES;
import static in.projecteka.consentmanager.userauth.Constants.PATH_USER_AUTH_INIT;
import static in.projecteka.consentmanager.userauth.Constants.PATH_USER_AUTH_CONFIRM;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
@RestController
public class UserAuthController {
    private final UserAuthentication userAuthentication;
    private final RequestValidator validator;

    private final Logger logger = LoggerFactory.getLogger(UserAuthController.class);

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_USER_AUTH_INIT)
    public Mono<Void> userAuthInit(@RequestBody AuthInitRequest request) {
        logger.info("Invoked User Auth Init API. Request Id {}", request.getRequestId());
        return just(request)
                .filterWhen(req -> validator.validate(request.getRequestId().toString(), request.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(r -> {
                    validator.put(request.getRequestId().toString(), request.getTimestamp());
                    return userAuthentication.authInit(request);
                });
    }

    @ResponseStatus(ACCEPTED)
    @PostMapping(PATH_USER_AUTH_CONFIRM)
    public Mono<Void> authOnConfirm(@RequestBody UserAuthConfirmRequest request) {
        logger.info("Invoked User Auth Confirm API. Request Id {}", request.getRequestId());
        return just(request)
                .filterWhen(req -> validator.validate(request.getRequestId(), request.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .doOnSuccess(validatedRequest -> Mono.defer(() -> {
                    validator.put(request.getRequestId(), request.getTimestamp());
                    return userAuthentication.authConfirm(request);
                }))
                .then();
    }

    @PostMapping(PATH_USER_FETCH_AUTH_MODES)
    @ResponseStatus(ACCEPTED)
    public Mono<Void> fetchAuthModes(@RequestBody FetchAuthModesRequest request){
        logger.info("Invoked User Auth Fetch Modes API. Request Id {}", request.getRequestId());
        return just(request)
                .filterWhen(req -> validator.validate(request.getRequestId().toString(), request.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(r -> {
                    validator.put(request.getRequestId().toString(), request.getTimestamp());
                    return userAuthentication.fetchAuthModes(request);
                });
    }
}
