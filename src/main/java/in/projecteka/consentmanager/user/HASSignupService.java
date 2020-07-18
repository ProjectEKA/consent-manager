package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.user.model.GrantType;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.SignUpResponse;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
import in.projecteka.consentmanager.user.model.UpdateLoginDetailsRequest;
import in.projecteka.consentmanager.user.model.UserCredential;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static in.projecteka.consentmanager.clients.ClientError.userAlreadyExists;

@AllArgsConstructor
public class HASSignupService {
    private final HASSignupServiceClient hasSignupServiceClient;
    private final UserRepository userRepository;
    private final SignUpService signUpService;
    private final TokenService tokenService;
    private final IdentityServiceClient identityServiceClient;
    private final SessionService sessionService;
    private final BCryptPasswordEncoder passwordEncoder;

    public Mono<SignUpResponse> createHASAccount(SignUpRequest signUpRequest, String token, String txnId) {
        var signupRequest = createHASSignupRequest(signUpRequest, token, txnId);
        return hasSignupServiceClient.createHASAccount(signupRequest)
                .flatMap(user -> signUpService.getMobileNumber(txnId)
                        .flatMap(mobileNumber -> userRepository.save(user, mobileNumber))
                        .then(signUpService.removeOf(txnId))
                        .thenReturn(SignUpResponse.builder().healthId(user.getHealthId()).token(user.getToken()).build())
                );
    }

    private HASSignupRequest createHASSignupRequest(SignUpRequest signUpRequest, String token, String txnId) {
        return HASSignupRequest.builder()
                .firstName(signUpRequest.getName().getFirst())
                .middleName(signUpRequest.getName().getMiddle())
                .lastName(signUpRequest.getName().getLast())
                .name(signUpRequest.getName().createFullName())
                .dayOfBirth(signUpRequest.getDateOfBirth().getDate())
                .monthOfBirth(signUpRequest.getDateOfBirth().getMonth())
                .yearOfBirth(signUpRequest.getDateOfBirth().getYear())
                .token(token)
                .txnId(txnId)
                .gender(signUpRequest.getGender().toString())
                .build();
    }

    public Mono<Session> updateHASLoginDetails(UpdateLoginDetailsRequest request, String token) {
        var updateHASLoginDetails = createUpdateHASUserRequest(request, token);
        return userRepository.userWith(request.getCmId())
                .flatMap(patient -> Mono.error(userAlreadyExists(patient.getIdentifier())))
                .switchIfEmpty(Mono.defer(() -> hasSignupServiceClient.updateHASAccount(updateHASLoginDetails)))
                .then(Mono.defer(() -> userRepository.updateCMId(request.getHealthId(), request.getCmId())))
                .then(Mono.defer(() -> userRepository.getNameByHealthId(request.getHealthId())))
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.userNotFound())))
                .flatMap(patientName -> Mono.just(new KeycloakUser(patientName.createFullName(),
                        request.getCmId(),
                        Collections.singletonList(new UserCredential(request.getPassword())),
                        Boolean.TRUE.toString()
                )))
                .flatMap(keycloakUser -> tokenService.tokenForAdmin()
                        .flatMap(accessToken -> identityServiceClient.createUser(accessToken, keycloakUser))
                        .onErrorResume(ClientError.class, error -> {
                            userRepository.updateCMId(request.getHealthId(), null);
                            return Mono.error(ClientError.userAlreadyExists(keycloakUser.getUsername()));
                        }))
                .then(Mono.defer(() -> sessionService.forNew(SessionRequest.builder().grantType(GrantType.PASSWORD)
                        .username(request.getCmId())
                        .password(request.getPassword())
                        .build())));
    }

    private UpdateHASUserRequest createUpdateHASUserRequest(UpdateLoginDetailsRequest request, String token) {
        return UpdateHASUserRequest.builder()
                .healthId(request.getHealthId())
                .password(encryptPassword(request.getPassword()))
                .token(token)
                .build();
    }

    private String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }
}
