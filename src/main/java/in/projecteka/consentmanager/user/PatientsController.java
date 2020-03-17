package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.user.model.CreatePinRequest;
import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.ValidatePinRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/patients")
@AllArgsConstructor
public class PatientsController {
    private Authenticator authenticator;
    private ProfileService profileService;
    private TransactionPinService transactionPinService;

    @PostMapping("/pin")
    public Mono<Void> pin(@RequestHeader(name = "Authorization") String token,
                                    @RequestBody CreatePinRequest createPinRequest) {
        return getUserNameFrom(token)
                .flatMap(userName -> transactionPinService.createPinFor(userName, createPinRequest.getPin()));
    }

    @GetMapping("/me")
    public Mono<Profile> profileFor(@RequestHeader(name = "Authorization") String token) {
        return getUserNameFrom(token)
                .flatMap(userName -> profileService.profileFor(userName));
    }

    @PostMapping(value = "/verify-pin")
    public Mono<Token> validatePin(@RequestHeader(value = "Authorization") String token,
                                   @RequestBody ValidatePinRequest request) {
        return getUserNameFrom(token)
                .flatMap(userName -> transactionPinService.validatePinFor(userName, request.getPin()));
    }

    private Mono<String> getUserNameFrom(@RequestHeader(name = "Authorization") String token) {
        return authenticator.userFrom(token).map(Caller::getUserName);
    }
}
