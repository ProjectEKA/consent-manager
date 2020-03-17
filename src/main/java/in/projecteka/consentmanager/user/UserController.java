package in.projecteka.consentmanager.user;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final SignUpService signupService;

    // TODO: only service accounts can invoke this, not an user
    // TODO: for hiu and hip we should have different URL where we don't return
    @GetMapping("/{userName}")
    public Mono<User> userWith(@PathVariable String userName) {
        return userService.userWith(userName);
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SignUpSession> sendOtp(@RequestBody UserSignUpEnquiry request) {
        return userService.sendOtp(request);
    }

    @PostMapping("/permit")
    public Mono<Token> permitOtp(@RequestBody OtpVerification request) {
        return userService.permitOtp(request);
    }

    @PostMapping
    public Mono<Session> create(@RequestBody SignUpRequest request,
                                @RequestHeader(name = "Authorization") String token) {
        return userService.create(request, signupService.sessionFrom(token));
    }
}
