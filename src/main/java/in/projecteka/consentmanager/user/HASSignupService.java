package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.HASSignupServiceClient;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.Serializer;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.GenerateAadharOtpRequest;
import in.projecteka.consentmanager.user.model.GenerateAadharOtpResponse;
import in.projecteka.consentmanager.user.model.GrantType;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.LoginResponse;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.SignUpResponse;
import in.projecteka.consentmanager.user.model.UpdateHASAddressRequest;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
import in.projecteka.consentmanager.user.model.UpdateLoginDetailsRequest;
import in.projecteka.consentmanager.user.model.UserCredential;
import in.projecteka.consentmanager.user.model.VerifyAadharOtpRequest;
import in.projecteka.consentmanager.user.model.VerifyAadharOtpResponse;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.ClientError.userAlreadyExists;

@AllArgsConstructor
public class HASSignupService {
    private final HASSignupServiceClient hasSignupServiceClient;
    private final UserRepository userRepository;
    private final SignUpService signUpService;
    private final TokenService tokenService;
    private final IdentityServiceClient identityServiceClient;
    private final SessionService sessionService;
    private final OtpServiceProperties otpServiceProperties;
    private final DummyHealthAccountService dummyHealthAccountService;
    private final CacheAdapter<String, String> hasCache;

    public Mono<SignUpResponse> createHASAccount(SignUpRequest signUpRequest, String token) {
        String sessionId = signUpService.getSessionId(token);
        return signUpService.getMobileNumber(sessionId)
                .flatMap(mobileNumber -> {
                    HASSignupRequest signupRequest = createHASSignupRequest(signUpRequest, token, sessionId);
                    return createAccount(signupRequest, mobileNumber)
                            .flatMap(healthAccountUser -> userRepository.getPatientByHealthId(healthAccountUser.getHealthId())
                                    .flatMap(user -> signUpService.removeOf(sessionId)
                                            .thenReturn(createSignUpResponse(healthAccountUser, user.getIdentifier())))
                                    .switchIfEmpty(Mono.defer(() -> userRepository.save(healthAccountUser, mobileNumber))
                                            .then(Mono.defer(() -> signUpService.removeOf(sessionId)))
                                            .thenReturn(createSignUpResponse(healthAccountUser, null))));
                });
    }

    private Mono<HealthAccountUser> createAccount(HASSignupRequest signupRequest, String mobileNumber) {
        if (!isNumberFromAllowedList(mobileNumber)) {
            return hasSignupServiceClient.createHASAccount(signupRequest);
        }
        return dummyHealthAccountService.createHASAccount(signupRequest);
    }

    private SignUpResponse createSignUpResponse(HealthAccountUser user, String cmId) {
        return createSignUpResponse(user,cmId,user.getToken());
    }

    private SignUpResponse createSignUpResponse(HealthAccountUser user, String cmId, String token) {
        return SignUpResponse.builder()
                .healthId(user.getHealthId())
                .token(token)
                .dateOfBirth(DateOfBirth.builder().date(user.getDayOfBirth())
                        .month(user.getMonthOfBirth()).year(user.getYearOfBirth()).build())
                .patientName(PatientName.builder().first(user.getFirstName())
                        .last(user.getLastName()).middle(user.getMiddleName()).build())
                .gender(Gender.valueOf(user.getGender()))
                .districtName(user.getDistrictName())
                .stateName(user.getStateName())
                .newHASUser(user.getNewHASUser())
                .cmId(cmId)
                .build();
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
                .stateCode(signUpRequest.getStateCode())
                .districtCode(signUpRequest.getDistrictCode())
                .build();
    }

    public Mono<LoginResponse> updateHASLoginDetails(UpdateLoginDetailsRequest request, String token) {
        var updateHASLoginDetails = createUpdateHASUserRequest(request, token);
        return userRepository.userWith(request.getCmId())
                .flatMap(patient -> Mono.error(userAlreadyExists(patient.getIdentifier())))
                .switchIfEmpty(Mono.defer(() -> {
                    if (isHealthAccountToken(token)) {
                        return hasSignupServiceClient.updateHASAccount(updateHASLoginDetails, token);
                    }
                    return Mono.empty();
                }))
                .then(Mono.defer(() -> userRepository.updateCMId(request.getHealthId(), request.getCmId())))
                .then(Mono.defer(() -> userRepository.getPatientByHealthId(request.getHealthId())))
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.userNotFound())))
                .flatMap(patient -> Mono.just(new KeycloakUser(patient.getName().createFullName(),
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
                        .build())));
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
                .password(request.getPassword())
                .build();
    }

    private boolean isNumberFromAllowedList(String mobileNumber) {
        return otpServiceProperties.allowListNumbers().stream().anyMatch(number ->
                number.equals(mobileNumber));
    }

    public Mono<GenerateAadharOtpResponse> generateAadharOtp(GenerateAadharOtpRequest request, String token) {
        var sessionId = signUpService.getSessionId(token);
        return isValidAadhar(request.getAadhaar()) ?
                signUpService.getMobileNumber(sessionId)
                        .filter(this::isNumberFromAllowedList)
                        .flatMap(bool -> Mono.just(dummyHealthAccountService.createDummyGenerateAadharOtpResponse(token)))
                        .switchIfEmpty(Mono.defer(() -> hasSignupServiceClient.generateAadharOtp(request)
                                .flatMap(response -> Mono.just(GenerateAadharOtpResponse.builder()
                                        .transactionId(response.getTransactionId())
                                        .token(token)
                                        .build())))) :
                Mono.error(ClientError.invalidRequester("Invalid aadhar number"));
    }

    private Boolean isValidAadhar(String aadhaar) {
        String regex = "^\\d{12}$";
        return aadhaar.matches(regex);
    }

    public Mono<VerifyAadharOtpResponse> verifyAadharOtp(VerifyAadharOtpRequest request, String token) {
        var sessionId = signUpService.getSessionId(token);
        return signUpService.getMobileNumber(sessionId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.invalidRequester("bad request"))))
                .flatMap(mobileNumber -> (isNumberFromAllowedList(mobileNumber) ?
                        Mono.just(dummyHealthAccountService.createHASUser()) :
                        hasSignupServiceClient.verifyAadharOtp(request))
                        .flatMap(response -> hasCache.put(response.getHealthId(), response.getNewHASUser().toString())
                                .then(userRepository.getPatientByHealthId(response.getHealthId())
                                        .flatMap(patient -> Mono.just(createVerifyAadharOtpResponse(response)))
                                        .switchIfEmpty(Mono.defer(() ->
                                                updateFirstName(response)
                                                        .flatMap(updatedUser -> userRepository.save(updatedUser, mobileNumber)))
                                                .thenReturn(createVerifyAadharOtpResponse(response))))))
                .flatMap(response -> signUpService.removeOf(sessionId).thenReturn(response));
    }

    private Mono<HealthAccountUser> updateFirstName(HealthAccountUser user) {
        return Mono.just(HealthAccountUser.builder()
                .name(user.getName())
                .firstName(user.getName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .healthId(user.getHealthId())
                .gender(user.getGender())
                .dayOfBirth(user.getDayOfBirth())
                .monthOfBirth(user.getMonthOfBirth())
                .yearOfBirth(user.getYearOfBirth())
                .stateCode(user.getStateCode())
                .districtCode(user.getDistrictCode())
                .newHASUser(user.getNewHASUser())
                .districtName(user.getDistrictName())
                .stateName(user.getStateName())
                .newHASUser(user.getNewHASUser())
                .build());
    }

    private VerifyAadharOtpResponse createVerifyAadharOtpResponse(HealthAccountUser user) {
        return VerifyAadharOtpResponse.builder()
                .healthId(user.getHealthId())
                .name(user.getName())
                .dateOfBirth(DateOfBirth.builder()
                        .date(user.getDayOfBirth())
                        .month(user.getMonthOfBirth())
                        .year(user.getYearOfBirth()).build())
                .gender(user.getGender())
                .token(user.getToken())
                .build();
    }

    public Mono<SignUpResponse> updateHASAddress(UpdateHASAddressRequest request, String token) {
        return hasCache.getIfPresent(request.getHealthId())
                .flatMap(newHasUser -> {
                    var isNewUser = Serializer.to(newHasUser, Boolean.class);
                    return Mono.just(isNewUser);
                })
                .flatMap(isNewUser ->
                        userRepository.getPatientByHealthId(request.getHealthId())
                                .flatMap(user -> isNumberFromAllowedList(user.getPhone()) && !isHealthAccountToken(token)
                                        ? userRepository.updateAddress(request)
                                        .then(Mono.just(createSignUpResponse(dummyHealthAccountService.mapToHealthAccountUser(user, isNewUser),
                                                user.getIdentifier())))
                                        : hasSignupServiceClient.updateHASAddress(request, token)
                                        .flatMap(this::updateFirstName)
                                        .flatMap(hasUser -> {
                                            userRepository.updateAddress(request);
                                            return Mono.just(createSignUpResponse(hasUser, user.getIdentifier(), token));
                                        })))
                .switchIfEmpty(Mono.error(ClientError.invalidRequester("Invalid Update Address Request")));
    }
}