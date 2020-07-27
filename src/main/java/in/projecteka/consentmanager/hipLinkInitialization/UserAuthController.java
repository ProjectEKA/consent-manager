package in.projecteka.consentmanager.hipLinkInitialization;

import in.projecteka.consentmanager.hipLinkInitialization.model.request.AuthInitRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.hipLinkInitialization.Constants.PATH_HIP_LINK_USER_AUTH_INIT;

@AllArgsConstructor
public class UserAuthController {
    private final UserAuthService userAuthService;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_HIP_LINK_USER_AUTH_INIT)
    public Mono<Void> userAuthInit(@RequestBody AuthInitRequest authInitRequest) {
        return userAuthService.authInit(authInitRequest);
    }
}

