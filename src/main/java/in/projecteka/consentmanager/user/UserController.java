package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final SignUpService signupService;

    // TODO: Should not return phone number from this API.
    @GetMapping("/users/{userName}")
    public Mono<User> userWith(@PathVariable String userName) {
        return userService.userWith(userName);
    }

    @PostMapping("/users/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SignUpSession> sendOtp(@RequestBody UserSignUpEnquiry request) {
        return userService.sendOtp(request);
    }

    @PostMapping("/users/permit")
    public Mono<Token> permitOtp(@RequestBody OtpVerification request) {
        return userService.permitOtp(request);
    }

    @PostMapping("/users")
    public Mono<Session> create(@RequestBody SignUpRequest request,
                                @RequestHeader(name = "Authorization") String token) {
        var signUpRequests = SignUpRequestValidator.validate(request);
        return signUpRequests.isValid()
               ? userService.create(signUpRequests.get(), signupService.sessionFrom(token))
               : Mono.error(new ClientError(HttpStatus.BAD_REQUEST,
                       new ErrorRepresentation(new Error(ErrorCode.INVALID_REQUESTER,
                               signUpRequests.getError().reduce((left, right) -> format("%s, %s", left, right))))));
    }

    // TODO: should be moved to patients and need to make sure only consent manager service uses it.
    // Not patient themselves
    @GetMapping("/internal/users/{userName}")
    public Mono<User> internalUserWith(@PathVariable String userName) {
        return userService.userWith(userName);
    }
}
