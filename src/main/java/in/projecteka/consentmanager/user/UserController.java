package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.DeviceIdentifier;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.TemporarySession;
import in.projecteka.consentmanager.user.model.User;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @GetMapping("/{userName}")
    public Mono<User> userWith(@PathVariable String userName) {
        return userService.userWith(userName);
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TemporarySession> sendOtp(@RequestBody DeviceIdentifier request) {
        return userService.sendOtp(request);
    }

    @PostMapping("/permit")
    public Mono<Token> permitOtp(@RequestBody OtpVerification request) {
        return userService.permitOtp(request);
    }

}
