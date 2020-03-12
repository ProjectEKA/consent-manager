package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.user.model.CreatePinRequest;
import in.projecteka.consentmanager.user.model.Profile;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/patients")
@AllArgsConstructor
public class PatientsController {
    private Authenticator authenticator;
    private ProfileService profileService;
    private TransactionPinService transactionPinService;

    @PostMapping("/pin")
    public Mono<ServerResponse> pin(@RequestHeader(name = "Authorization") String token,
                                    @RequestBody CreatePinRequest createPinRequest) {
        return getUserNameFrom(token) //TODO : Add token validation (token to be 6 digit integer)
                .flatMap(userName -> transactionPinService.createPinFor(userName, createPinRequest.getPin()))
                .then(ServerResponse.status(HttpStatus.CREATED).body(BodyInserters.empty()));
    }

    @PostMapping("/me")
    public Mono<Profile> me(@RequestHeader(name = "Authorization") String token) {
        return getUserNameFrom(token)
                .flatMap(userName -> profileService.getProfileFor(userName));
    }

    private Mono<String> getUserNameFrom(@RequestHeader(name = "Authorization") String token) {
        return authenticator.userFrom(token).map(Caller::getUserName);
    }
}
