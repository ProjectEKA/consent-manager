package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.consent.model.Action;
import in.projecteka.consentmanager.consent.model.Communication;
import in.projecteka.consentmanager.consent.model.CommunicationType;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.filters.ABPMJAYIdFilter;
import in.projecteka.consentmanager.user.filters.NameFilter;
import in.projecteka.consentmanager.user.filters.YOBFilter;
import in.projecteka.consentmanager.user.model.*;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.ClientError.failedToFetchUserCredentials;
import static in.projecteka.consentmanager.clients.ClientError.from;
import static in.projecteka.consentmanager.clients.ClientError.userAlreadyExists;
import static in.projecteka.consentmanager.clients.ClientError.userNotFound;
import static in.projecteka.consentmanager.user.IdentifierUtils.getIdentifierValue;
import static in.projecteka.consentmanager.user.model.IdentifierType.MOBILE;
import static java.lang.String.format;

@AllArgsConstructor
public class UserService {
    public static final String INVALID_REQUEST_BODY = "invalid.request.body";
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final OtpServiceProperties otpServiceProperties;
    private final OtpServiceClient otpServiceClient;
    private final SignUpService signupService;
    private final IdentityServiceClient identityServiceClient;
    private final TokenService tokenService;
    private final UserServiceProperties userServiceProperties;
    private final OtpAttemptService otpAttemptService;
    private final LockedUserService lockedUserService;
    private final UserServiceClient userServiceClient;

    public Mono<User> userWith(String userName) {
        return userRepository.userWith(userName.toLowerCase()).switchIfEmpty(Mono.error(userNotFound()));
    }

    public Mono<Void> user(String userName, RequesterDetail requester, UUID requestId) {
        return Mono.defer(() -> {
            findUser(userName, requester.getType().getRoutingKey(), requester.getId(), requestId);
            return Mono.empty();
        });
    }

    private void findUser(String userName, String routingKey, String requesterId, UUID requestId) {
        userWith(userName)
                .map(user -> {
                    Patient patient = Patient.builder()
                            .id(user.getIdentifier())
                            .name(user.getName().createFullName())
                            .build();
                    var patientResponse = PatientResponse.builder()
                            .requestId(UUID.randomUUID())
                            .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                            .patient(patient)
                            .resp(GatewayResponse.builder().requestId(requestId.toString()).build())
                            .build();
                    logger.info(format("patient Response %s", patientResponse.toString()));
                    return patientResponse;
                })
                .onErrorResume(ClientError.class, exception -> {
                    var patientResponse = PatientResponse.builder()
                            .requestId(UUID.randomUUID())
                            .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                            .error(from(exception))
                            .resp(GatewayResponse.builder().requestId(requestId.toString()).build())
                            .build();
                    logger.error(exception.getMessage(), exception);
                    return Mono.just(patientResponse);
                })
                .flatMap(patientResponse -> userServiceClient.sendPatientResponseToGateWay(patientResponse,
                        routingKey,
                        requesterId))
                .subscribe();
    }

    public Mono<SignUpSession> sendOtp(UserSignUpEnquiry userSignupEnquiry) {
        return getOtpRequest(userSignupEnquiry)
                .map(otpRequest -> otpAttemptService
                        .validateOTPRequest(userSignupEnquiry.getIdentifierType(),
                                userSignupEnquiry.getIdentifier(),
                                OtpAttempt.Action.OTP_REQUEST_REGISTRATION)
                        .then(otpServiceClient.send(otpRequest))
                        .then(signupService.cacheAndSendSession(otpRequest.getSessionId(),
                                otpRequest.getCommunication().getValue())))
                .orElse(Mono.error(new InvalidRequestException("invalid.identifier.type")));
    }

    private Optional<OtpRequest> getOtpRequest(UserSignUpEnquiry userSignupEnquiry) {
        String identifierType = userSignupEnquiry.getIdentifierType().toUpperCase();
        if (!otpServiceProperties.getIdentifiers().contains(identifierType)) {
            return Optional.empty();
        }
        var communication = new OtpCommunicationData(userSignupEnquiry.getIdentifierType(),
                userSignupEnquiry.getIdentifier());
        var otpRequest = new OtpRequest(UUID.randomUUID().toString(), communication);
        return Optional.of(otpRequest);
    }

    public Mono<SignUpSession> sendOtpFor(UserSignUpEnquiry userSignupEnquiry,
                                          String userName,
                                          OtpAttempt.Action otpAttemptAction,
                                          SendOtpAction sendOtpAction) {
        return getOtpRequest(userSignupEnquiry)
                .map(otpRequest -> otpAttemptService
                        .validateOTPRequest(userSignupEnquiry.getIdentifierType().toUpperCase(),
                                userSignupEnquiry.getIdentifier(),
                                otpAttemptAction,
                                userName)
                        .then(otpServiceClient.send(otpRequest))
                        .then(signupService.updatedVerifiedSession(otpRequest.getSessionId(), userName, sendOtpAction)))
                .orElse(Mono.error(new InvalidRequestException("invalid.identifier.type")));
    }

    private Mono<Void> validateAndVerifyOtp(OtpVerification otpVerification, OtpAttempt attempt) {
        return otpAttemptService.validateOTPSubmission(attempt)
                .then(otpServiceClient.verify(otpVerification.getSessionId(), otpVerification.getValue()))
                .onErrorResume(ClientError.class, error -> otpAttemptService.handleInvalidOTPError(error, attempt))
                .then(otpAttemptService.removeMatchingAttempts(attempt));
    }

    public Mono<Token> verifyOtpForRegistration(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            return Mono.error(new InvalidRequestException(INVALID_REQUEST_BODY));
        }
        return signupService.getMobileNumber(otpVerification.getSessionId())
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(mobileNumber -> {
                    OtpAttempt.OtpAttemptBuilder builder = OtpAttempt.builder()
                            .sessionId(otpVerification.getSessionId())
                            .identifierType(MOBILE.name())
                            .identifierValue(mobileNumber)
                            .action(OtpAttempt.Action.OTP_SUBMIT_REGISTRATION);
                    return validateAndVerifyOtp(otpVerification, builder.build())
                            .then(signupService.generateToken(otpVerification.getSessionId()));
                });
    }

    public Mono<Token> verifyOtpForForgetPassword(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            return Mono.error(new InvalidRequestException(INVALID_REQUEST_BODY));
        }
        String sessionIdWithAction = SendOtpAction.RECOVER_PASSWORD.toString() + otpVerification.getSessionId();
        return signupService.getUserName(sessionIdWithAction)
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(userName -> lockedUserService.validateLogin(userName)
                        .then(otpServiceClient.verify(otpVerification.getSessionId(), otpVerification.getValue()))
                        .onErrorResume(ClientError.class, error ->
                        {
                            if (error.getErrorCode() == ErrorCode.OTP_INVALID) {
                                return lockedUserService.createOrUpdateLockedUser(userName).then(Mono.error(error));
                            }
                            return Mono.error(error);
                        })
                        .then(lockedUserService.removeLockedUser(userName))
                        .then(signupService.generateToken(new HashMap<>(), sessionIdWithAction)));
    }

    public Mono<RecoverCmIdResponse> verifyOtpForRecoverCmId(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            return Mono.error(new InvalidRequestException(INVALID_REQUEST_BODY));
        }
        String sessionIdWithAction = SendOtpAction.RECOVER_CM_ID.toString() + otpVerification.getSessionId();
        return signupService.getUserName(sessionIdWithAction)
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(userRepository::userWith)
                .flatMap(user -> {
                    OtpAttempt.OtpAttemptBuilder builder = OtpAttempt.builder()
                            .sessionId(otpVerification.getSessionId())
                            .identifierType(MOBILE.name())
                            .identifierValue(user.getPhone())
                            .action(OtpAttempt.Action.OTP_SUBMIT_RECOVER_CM_ID)
                            .cmId(user.getIdentifier());
                    return validateAndVerifyOtp(otpVerification, builder.build())
                            .then(Mono.just(createNotificationMessage(user, otpVerification.getSessionId()).flatMap(this::notifyUserWith)))
                            .then(Mono.just(RecoverCmIdResponse.builder().cmId(user.getIdentifier()).build()));
                });
    }

    private Mono<ConsentManagerIdNotification> createNotificationMessage(User user, String sessionId) {
        return Mono.just(ConsentManagerIdNotification.builder()
                .communication(Communication.builder()
                        .communicationType(CommunicationType.MOBILE)
                        .value(user.getPhone())
                        .build())
                .id(sessionId)
                .action(Action.CONSENT_MANAGER_ID_RECOVERED)
                .content(ConsentManagerIdContent.builder()
                        .consentManagerId(user.getIdentifier())
                        .build())
                .build());
    }

    public Mono<Void> notifyUserWith(ConsentManagerIdNotification consentManagerIdNotification) {
        return otpServiceClient.send(consentManagerIdNotification);
    }

    public Mono<Void> create(CoreSignUpRequest coreSignUpRequest, String sessionId) {
        UserCredential credential = new UserCredential(coreSignUpRequest.getPassword());
        KeycloakUser keycloakUser = new KeycloakUser(
                coreSignUpRequest.getName().createFullName(),
                coreSignUpRequest.getUsername(),
                Collections.singletonList(credential),
                Boolean.TRUE.toString());

        return signupService.getMobileNumber(sessionId)
                .switchIfEmpty(Mono.error(new InvalidRequestException("mobile number not verified")))
                .flatMap(mobileNumber -> userExistsWith(coreSignUpRequest.getUsername())
                        .switchIfEmpty(Mono.defer(() -> createUserWith(mobileNumber, coreSignUpRequest, keycloakUser)))
                        .then());
    }

    public Mono<Session> update(UpdateUserRequest updateUserRequest, String sessionId) {
        return signupService.getUserName(sessionId)
                .switchIfEmpty(Mono.error(new InvalidRequestException("user not verified")))
                .flatMap(userName -> updatedSessionFor(updateUserRequest.getPassword(), userName));
    }

    public Mono<Session> updatePassword(UpdatePasswordRequest request, String userName) {
        return tokenService.tokenForUser(userName, request.getOldPassword())
                .onErrorResume(error -> lockedUserService.createOrUpdateLockedUser(userName)
                        .flatMap(attempts -> Mono.error(ClientError.invalidOldPassword(attempts))))
                .flatMap(session -> lockedUserService.removeLockedUser(userName)
                        .then(updatedSessionFor(request.getNewPassword(), userName)));
    }

    private Mono<Session> updatedSessionFor(String password, String userName) {
        return tokenService
                .tokenForAdmin()
                .flatMap(adminSession -> {
                    String accessToken = format("Bearer %s", adminSession.getAccessToken());
                    return identityServiceClient.getUser(userName, accessToken)
                            .map(user -> Tuples.of(user, accessToken));
                })
                .flatMap(userToken -> identityServiceClient.updateUser(userToken.getT2(),
                        userToken.getT1().getId(),
                        password))
                .doOnError(error -> Mono.error(ClientError.failedToUpdateUser()))
                .then(tokenService.tokenForUser(userName, password));
    }

    public Mono<LoginModeResponse> getLoginMode(String userName) {
        return tokenService.tokenForAdmin()
                .flatMap(adminSession -> {
                    String accessToken = format("Bearer %s", adminSession.getAccessToken());
                    return identityServiceClient.getUser(userName, accessToken)
                            .map(user -> Tuples.of(user, accessToken));
                })
                .switchIfEmpty(Mono.error(userNotFound()))
                .flatMap(userToken -> identityServiceClient
                        .getCredentials(userToken.getT1().getId(), userToken.getT2()))
                .doOnError(error -> Mono.error(failedToFetchUserCredentials()))
                .map(discard -> LoginModeResponse.builder().loginMode(LoginMode.CREDENTIAL).build())
                .switchIfEmpty(Mono.just(LoginModeResponse.builder().loginMode(LoginMode.OTP).build()));
    }

    private Mono<Object> userExistsWith(String username) {
        return userRepository.userWith(username)
                .flatMap(patient -> {
                    logger.error(format("Patient with %s already exists", patient.getIdentifier()));
                    return Mono.error(userAlreadyExists(patient.getIdentifier()));
                });
    }

    private Mono<Void> createUserWith(String mobileNumber,
                                      CoreSignUpRequest coreSignUpRequest,
                                      KeycloakUser keycloakUser) {
        User user = User.from(coreSignUpRequest, mobileNumber);
        return userRepository.save(user)
                .then(tokenService.tokenForAdmin()
                        .flatMap(accessToken -> identityServiceClient.createUser(accessToken, keycloakUser))
                        .then())
                .onErrorResume(ClientError.class, error -> {
                    logger.error(error.getMessage(), error);
                    return userRepository.delete(user.getIdentifier()).then();
                })
                .then();
    }

    private boolean validateOtpVerification(OtpVerification otpVerification) {
        return otpVerification.getSessionId() != null
                && !otpVerification.getSessionId().isEmpty()
                && otpVerification.getValue() != null
                && !otpVerification.getValue().isEmpty();
    }

    public String getUserIdSuffix() {
        return userServiceProperties.getUserIdSuffix();
    }

    public int getExpiryInMinutes() {
        return otpServiceProperties.getExpiryInMinutes();
    }

    public Mono<User> getPatientByDetails(InitiateCmIdRecoveryRequest request) {
        return userRepository
                .getUserBy(request.getGender(), getIdentifierValue(request.getVerifiedIdentifiers(), MOBILE))
                .collectList()
                .flatMap(users -> new NameFilter().filter(users, request.getName()))
                .flatMap(users -> new YOBFilter().filter(users, request.getDateOfBirth()))
                .flatMap(users -> new ABPMJAYIdFilter().filter(users, request.getUnverifiedIdentifiers()))
                .flatMap(this::getDistinctUser);
    }

    private Mono<User> getDistinctUser(List<User> rows) {
        return rows.size() == 1 ? Mono.just(rows.get(0)) : Mono.empty();
    }
}
