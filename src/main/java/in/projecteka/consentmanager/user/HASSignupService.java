package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.user.model.GrantType;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.SignUpResponse;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
import in.projecteka.consentmanager.user.model.UpdateLoginDetailsRequest;
import in.projecteka.consentmanager.user.model.UserCredential;
import in.projecteka.consentmanager.user.model.UpdateLoginDetailsResponse;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.ClientError.userAlreadyExists;
import static java.lang.String.format;

@AllArgsConstructor
public class HASSignupService {
    private final HASSignupServiceClient hasSignupServiceClient;
    private final UserRepository userRepository;
    private final SignUpService signUpService;
    private final TokenService tokenService;
    private final IdentityServiceClient identityServiceClient;
    private final SessionService sessionService;
    private final OtpServiceProperties otpServiceProperties;

    public Mono<SignUpResponse> createHASAccount(SignUpRequest signUpRequest, String token) {
        String sessionId = signUpService.getSessionId(token);
        return signUpService.getMobileNumber(sessionId)
                .flatMap(mobileNumber -> {
                    HASSignupRequest signupRequest = createHASSignupRequest(signUpRequest, token, sessionId);
                    return createAccount(signupRequest, mobileNumber)
                            .flatMap(healthAccountUser -> userRepository.save(healthAccountUser, mobileNumber)
                                    .then(signUpService.removeOf(sessionId))
                                    .thenReturn(SignUpResponse.builder().healthId(healthAccountUser.getHealthId())
                                            .token(healthAccountUser.getToken()).build()));
                });
    }

    private Mono<HealthAccountUser> createAccount(HASSignupRequest signupRequest, String mobileNumber) {
        if (!isNumberFromAllowedList(mobileNumber)) {
            return hasSignupServiceClient.createHASAccount(signupRequest);
        }
        return Mono.just(
                HealthAccountUser.builder()
                .firstName(signupRequest.getFirstName())
                .middleName(signupRequest.getMiddleName())
                .lastName(signupRequest.getLastName())
                .gender(signupRequest.getGender())
                .dayOfBirth(signupRequest.getDayOfBirth())
                .monthOfBirth(signupRequest.getMonthOfBirth())
                .yearOfBirth(signupRequest.getYearOfBirth())
                .healthId(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString())
                .build()
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

    public Mono<UpdateLoginDetailsResponse> updateHASLoginDetails(UpdateLoginDetailsRequest request, String token) {
        var updateHASLoginDetails = createUpdateHASUserRequest(request, token);
        return userRepository.userWith(request.getCmId())
                .flatMap(patient -> Mono.error(userAlreadyExists(patient.getIdentifier())))
                .switchIfEmpty(Mono.defer(() -> {
                    if (isHealthAccountToken(token)) {
                        return hasSignupServiceClient.updateHASAccount(updateHASLoginDetails);
                    }
                    return Mono.empty();
                }))
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
                        .onErrorResume(ClientError.class, error ->
                                userRepository.updateCMId(request.getHealthId(), null)
                                        .then(Mono.error(ClientError.userAlreadyExists(keycloakUser.getUsername())))
                        ))
                .then(Mono.defer(() -> sessionService.forNew(SessionRequest.builder().grantType(GrantType.PASSWORD)
                        .username(request.getCmId())
                        .password(request.getPassword())
                        .build())))
                .flatMap(session -> {
                    String accessToken = format("Bearer %s", session.getAccessToken());
                    return Mono.just(UpdateLoginDetailsResponse.builder().token(accessToken).build());
                });
    }

    private boolean isHealthAccountToken(String token) {
        try {
            UUID.fromString(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private UpdateHASUserRequest createUpdateHASUserRequest(UpdateLoginDetailsRequest request, String token) {
        return UpdateHASUserRequest.builder()
                .healthId(request.getHealthId())
                .password(request.getPassword())
                .token(token)
                .build();
    }

    private boolean isNumberFromAllowedList(String mobileNumber) {
        return otpServiceProperties.allowListNumbers().stream().anyMatch(number ->
                number.equals(mobileNumber));
    }
}
