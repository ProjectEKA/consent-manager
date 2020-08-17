package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.OtpAction;
import in.projecteka.consentmanager.consent.ConsentServiceProperties;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.consentmanager.properties.OtpServiceProperties;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.filters.ABPMJAYIdFilter;
import in.projecteka.consentmanager.user.filters.NameFilter;
import in.projecteka.consentmanager.user.filters.YOBFilter;
import in.projecteka.consentmanager.user.model.CoreSignUpRequest;
import in.projecteka.consentmanager.user.model.InitiateCmIdRecoveryRequest;
import in.projecteka.consentmanager.user.model.LoginMode;
import in.projecteka.consentmanager.user.model.LoginModeResponse;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.Patient;
import in.projecteka.consentmanager.user.model.PatientResponse;
import in.projecteka.consentmanager.user.model.RecoverCmIdResponse;
import in.projecteka.consentmanager.user.model.RequesterDetail;
import in.projecteka.consentmanager.user.model.SendOtpAction;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdatePasswordRequest;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserAuthConfirmRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.library.clients.IdentityServiceClient;
import in.projecteka.library.clients.OtpServiceClient;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.Communication;
import in.projecteka.library.clients.model.CommunicationType;
import in.projecteka.library.clients.model.ErrorCode;
import in.projecteka.library.clients.model.KeycloakUser;
import in.projecteka.library.clients.model.Notification;
import in.projecteka.library.clients.model.OtpCommunicationData;
import in.projecteka.library.clients.model.OtpGenerationDetail;
import in.projecteka.library.clients.model.OtpRequest;
import in.projecteka.library.clients.model.Session;
import in.projecteka.library.clients.model.UserCredential;
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

import static in.projecteka.consentmanager.user.IdentifierUtils.getIdentifierValue;
import static in.projecteka.consentmanager.user.model.IdentifierType.MOBILE;
import static in.projecteka.library.clients.model.Action.CONSENT_MANAGER_ID_RECOVERED;
import static in.projecteka.library.clients.model.ClientError.failedToFetchUserCredentials;
import static in.projecteka.library.clients.model.ClientError.from;
import static in.projecteka.library.clients.model.ClientError.userAlreadyExists;
import static in.projecteka.library.clients.model.ClientError.userNotFound;
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

    private final ConsentServiceProperties consentServiceProperties;

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
        return getOtpRequest(userSignupEnquiry, OtpAttempt.Action.OTP_REQUEST_REGISTRATION)
                .map(otpRequest -> otpAttemptService
                        .validateOTPRequest(userSignupEnquiry.getIdentifierType(),
                                userSignupEnquiry.getIdentifier(),
                                OtpAttempt.Action.OTP_REQUEST_REGISTRATION)
                        .then(otpServiceClient.send(otpRequest))
                        .then(signupService.cacheAndSendSession(otpRequest.getSessionId(),
                                otpRequest.getCommunication().getValue())))
                .orElse(Mono.error(new InvalidRequestException("invalid.identifier.type")));
    }

    private Optional<OtpRequest> getOtpRequest(UserSignUpEnquiry userSignupEnquiry, OtpAttempt.Action otpAttmptAction) {
        String identifierType = userSignupEnquiry.getIdentifierType().toUpperCase();
        if (!otpServiceProperties.getIdentifiers().contains(identifierType)) {
            return Optional.empty();
        }
        var communication = new OtpCommunicationData(userSignupEnquiry.getIdentifierType(),
                userSignupEnquiry.getIdentifier());

        OtpGenerationDetail otpGenerationDetail = OtpGenerationDetail
                .builder()
                .action(getOtpActionFor(otpAttmptAction).toString())
                .systemName(consentServiceProperties.getName())
                .build();
        var otpRequest = new OtpRequest(UUID.randomUUID().toString(), communication, otpGenerationDetail);
        return Optional.of(otpRequest);
    }

    private OtpAction getOtpActionFor(OtpAttempt.Action otpAttmptAction) {
        switch (otpAttmptAction) {
            case OTP_REQUEST_REGISTRATION:
                return OtpAction.REGISTRATION;
            case OTP_REQUEST_LOGIN:
                return OtpAction.LOGIN;
            case OTP_REQUEST_RECOVER_PASSWORD:
                return OtpAction.RECOVER_PASSWORD;
            case OTP_REQUEST_FORGOT_CONSENT_PIN:
                return OtpAction.FORGOT_PIN;
            default:
                return OtpAction.FORGOT_CM_ID;
        }
    }

    public Mono<SignUpSession> sendOtpFor(UserSignUpEnquiry userSignupEnquiry,
                                          String userName,
                                          OtpAttempt.Action otpAttemptAction,
                                          SendOtpAction sendOtpAction) {
        return getOtpRequest(userSignupEnquiry, otpAttemptAction)
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
                            .then(createNotificationMessage(user, otpVerification.getSessionId())
                                    .flatMap(this::notifyUserWith))
                            .then(Mono.just(RecoverCmIdResponse.builder().cmId(user.getIdentifier()).build()));
                });
    }

    public Mono<Token> verifyOtpForForgotConsentPin(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            return Mono.error(new InvalidRequestException(INVALID_REQUEST_BODY));
        }
        String sessionIdWithAction = SendOtpAction.FORGOT_CONSENT_PIN.toString() + otpVerification.getSessionId();
        return signupService.getUserName(sessionIdWithAction)
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(userRepository::userWith)
                .flatMap(user -> {
                    OtpAttempt otpAttempt = OtpAttempt.builder()
                            .sessionId(otpVerification.getSessionId())
                            .identifierType(MOBILE.name())
                            .identifierValue(user.getPhone())
                            .action(OtpAttempt.Action.OTP_SUBMIT_FORGOT_CONSENT_PIN)
                            .cmId(user.getIdentifier())
                            .build();
                    return validateAndVerifyOtp(otpVerification, otpAttempt)
                            .then(signupService.generateToken(new HashMap<>(), sessionIdWithAction));
                });
    }

    private Mono<Notification<ConsentManagerIdContent>> createNotificationMessage(User user, String sessionId) {
        return Mono.just(new Notification<>(sessionId,
                Communication.builder()
                        .communicationType(CommunicationType.MOBILE)
                        .value(user.getPhone())
                        .build(),
                ConsentManagerIdContent.builder()
                        .consentManagerId(user.getIdentifier())
                        .build(),
                CONSENT_MANAGER_ID_RECOVERED));
    }

    public Mono<Void> notifyUserWith(Notification<ConsentManagerIdContent> consentManagerIdNotification) {
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

    public Mono<Void> confirmAuthFor(UserAuthConfirmRequest request) {
        return Mono.empty();
    }
}
