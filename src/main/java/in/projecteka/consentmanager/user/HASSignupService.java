package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.SignUpResponse;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class HASSignupService {
    private final HASSignupServiceClient serviceClient;
    private final UserRepository userRepository;
    private final SignUpService signUpService;


    public Mono<SignUpResponse> createHASAccount(SignUpRequest signUpRequest, String token, String txnId) {
        var signupRequest = createHASSignupRequest(signUpRequest, token);
        return serviceClient.createHASAccount(signupRequest)
                .flatMap(user -> {
                    System.out.println(user.toString() + " ----------");
                            return signUpService.getMobileNumber(txnId)
                                    .flatMap(mobileNumber -> userRepository.save(user, mobileNumber))
                                    .then(signUpService.removeOf(txnId))
                                    .thenReturn(SignUpResponse.builder().healthId(user.getHealthId()).token(user.getToken()).build());
                        }
                );
    }

    private HASSignupRequest createHASSignupRequest(SignUpRequest signUpRequest, String token) {
        return HASSignupRequest.builder()
                .firstName(signUpRequest.getName().getFirst())
                .middleName(signUpRequest.getName().getMiddle())
                .lastName(signUpRequest.getName().getLast())
                .name(signUpRequest.getName().createFullName())
                .dayOfBirth(signUpRequest.getDateOfBirth().getDate())
                .monthOfBirth(signUpRequest.getDateOfBirth().getMonth())
                .yearOfBirth(signUpRequest.getDateOfBirth().getYear())
                .token(token)
                .build();
    }
}
