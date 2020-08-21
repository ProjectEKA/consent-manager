package in.projecteka.consentmanager.link.hiplink;

import in.projecteka.consentmanager.link.hiplink.model.request.AuthInitRequest;
import in.projecteka.consentmanager.user.Constants;
import in.projecteka.consentmanager.user.model.UserAuthConfirmRequest;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.RequestValidator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.link.Constants.PATH_HIP_LINK_USER_AUTH_INIT;
import static org.springframework.http.HttpStatus.ACCEPTED;

@AllArgsConstructor
public class UserAuthController {
    private final UserAuthentication userAuthentication;
    private final RequestValidator validator;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_HIP_LINK_USER_AUTH_INIT)
    public Mono<Void> userAuthInit(@RequestBody AuthInitRequest authInitRequest) {
        return userAuthentication.authInit(authInitRequest);
    }

    @ResponseStatus(ACCEPTED)
    @PostMapping(Constants.USERS_AUTH_CONFIRM)
    public Mono<Void> authOnConfirm(@RequestBody UserAuthConfirmRequest request) {
        return Mono.just(request)
                .filterWhen(req -> validator.validate(request.getRequestId(), request.getTimestamp()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .doOnSuccess(validatedRequest -> Mono.defer(() -> {
                    validator.put(request.getRequestId(), request.getTimestamp());
                    return userAuthentication.confirmAuthFor(validatedRequest);
                }))
                .then();
    }
}
