package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.RequestValidator;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.PatientRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static in.projecteka.consentmanager.user.Constants.PATH_FIND_PATIENT;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final CacheAdapter<String, String> cacheForReplayAttack;
    private final RequestValidator validator;

    // TODO: Should not return phone number from this API.
    @GetMapping(Constants.APP_PATH_FIND_BY_USER_NAME)
    public Mono<User> userWith(@PathVariable String userName) {
        return userService.userWith(userName);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(PATH_FIND_PATIENT)
    public Mono<Void> userWith(@Valid @RequestBody PatientRequest patientRequest) {
        return Mono.just(patientRequest)
                .filterWhen(req ->
                        validator.validate(patientRequest.getRequestId().toString(), patientRequest.getTimestamp().toString()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(validatedRequest -> userService.user(patientRequest.getQuery().getPatient().getId(),
                                             patientRequest.getQuery().getRequester(),
                                             patientRequest.getRequestId())
                        .then(cacheForReplayAttack.put(patientRequest.getRequestId().toString(), patientRequest.getTimestamp().toString())));
    }

    @ResponseStatus(CREATED)
    @PostMapping(Constants.APP_PATH_USER_SIGN_UP_ENQUIRY)
    public Mono<SignUpSession> sendOtp(@RequestBody UserSignUpEnquiry request) {
        return userService.sendOtp(request);
    }

    @PostMapping("/users/permit")
    public Mono<Token> permitOtp(@RequestBody OtpVerification request) {
        return userService.verifyOtpForRegistration(request);
    }

    // TODO: should be moved to patients and need to make sure only consent manager service uses it.
    // Not patient themselves
    @GetMapping(Constants.APP_PATH_INTERNAL_FIND_USER_BY_USERNAME)
    public Mono<User> internalUserWith(@PathVariable String userName) {
        return userService.userWith(userName);
    }
}
