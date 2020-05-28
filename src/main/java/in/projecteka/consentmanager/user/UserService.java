package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.filters.ABPMJAYIdFilter;
import in.projecteka.consentmanager.user.filters.NameFilter;
import in.projecteka.consentmanager.user.filters.YOBFilter;
import in.projecteka.consentmanager.user.model.CoreSignUpRequest;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.LoginMode;
import in.projecteka.consentmanager.user.model.LoginModeResponse;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.InitiateCmIdRecoveryRequest;
import in.projecteka.consentmanager.user.model.RecoverCmIdResponse;
import in.projecteka.consentmanager.user.model.SendOtpAction;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdatePasswordRequest;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserCredential;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.ClientError.failedToFetchUserCredentials;
import static in.projecteka.consentmanager.clients.ClientError.userAlreadyExists;
import static in.projecteka.consentmanager.clients.ClientError.userNotFound;
import static java.lang.String.format;

@AllArgsConstructor
public class UserService {
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

    public Mono<User> userWith(String userName) {
        return userRepository.userWith(userName.toLowerCase()).switchIfEmpty(Mono.error(userNotFound()));
    }

    public Mono<SignUpSession> sendOtp(UserSignUpEnquiry userSignupEnquiry) {
        String identifierType = userSignupEnquiry.getIdentifierType().toUpperCase();

        if (!otpServiceProperties.getIdentifiers().contains(identifierType)) {
            throw new InvalidRequestException("invalid.identifier.type");
        }

        String sessionId = UUID.randomUUID().toString();
        OtpRequest otpRequest = new OtpRequest(
                sessionId,
                new OtpCommunicationData(userSignupEnquiry.getIdentifierType(), userSignupEnquiry.getIdentifier()));

        return otpAttemptService
                .validateOTPRequest(userSignupEnquiry.getIdentifierType(), userSignupEnquiry.getIdentifier(), OtpAttempt.Action.OTP_REQUEST_REGISTRATION)
                .then(otpServiceClient.send(otpRequest)
                        .then(signupService.cacheAndSendSession(
                                otpRequest.getSessionId(),
                                otpRequest.getCommunication().getValue())));
    }

    public Mono<SignUpSession> sendOtpFor(UserSignUpEnquiry userSignupEnquiry, String userName, OtpAttempt.Action otpAttemtpAction, SendOtpAction sendOtpAction) {
        String identifierType = userSignupEnquiry.getIdentifierType().toUpperCase();

        if (!otpServiceProperties.getIdentifiers().contains(identifierType)) {
            throw new InvalidRequestException("invalid.identifier.type");
        }

        String sessionId = UUID.randomUUID().toString();
        OtpRequest otpRequest = new OtpRequest(
                sessionId,
                new OtpCommunicationData(userSignupEnquiry.getIdentifierType(), userSignupEnquiry.getIdentifier()));

        return otpAttemptService.validateOTPRequest(identifierType, userSignupEnquiry.getIdentifier(), otpAttemtpAction, userName)
                .then(otpServiceClient
                        .send(otpRequest)
                        .then(signupService.updatedVerfiedSession(
                                otpRequest.getSessionId(),
                                userName,
                                sendOtpAction)));
    }

    private Mono<Void> validateAndVerifyOtp(OtpVerification otpVerification, OtpAttempt attempt) {
        return otpAttemptService.validateOTPSubmission(attempt)
                .then(otpServiceClient.verify(otpVerification.getSessionId(), otpVerification.getValue()))
                .onErrorResume(ClientError.class, (error) -> otpAttemptService.handleInvalidOTPError(error, attempt))
                .then(otpAttemptService.removeMatchingAttempts(attempt));
    }

    public Mono<Token> verifyOtpForRegistration(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            throw new InvalidRequestException("invalid.request.body");
        }

        return signupService.getMobileNumber(otpVerification.getSessionId())
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(mobileNumber -> {
                    OtpAttempt.OtpAttemptBuilder builder = OtpAttempt.builder()
                            .sessionId(otpVerification.getSessionId())
                            .identifierType(IdentifierType.MOBILE.name())
                            .identifierValue(mobileNumber)
                            .action(OtpAttempt.Action.OTP_SUBMIT_REGISTRATION);
                    return validateAndVerifyOtp(otpVerification, builder.build())
                            .then(signupService.generateToken(otpVerification.getSessionId()));
                });
    }

    public Mono<Token> verifyOtpForForgetPassword(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            throw new InvalidRequestException("invalid.request.body");
        }
        String sessionIdWithAction = SendOtpAction.RECOVER_PASSWORD.toString() + otpVerification.getSessionId();
        return signupService.getUserName(sessionIdWithAction)
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(userName -> lockedUserService.validateLogin(userName)
                        .then(otpServiceClient.verify(otpVerification.getSessionId(), otpVerification.getValue()))
                        .onErrorResume(ClientError.class, (error) ->
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
            throw new InvalidRequestException("invalid.request.body");
        }
        String sessionIdWithAction = SendOtpAction.RECOVER_CM_ID.toString() + otpVerification.getSessionId();
        return signupService.getUserName(sessionIdWithAction)
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(userRepository::userWith)
                .flatMap(user -> {
                    OtpAttempt.OtpAttemptBuilder builder = OtpAttempt.builder()
                            .sessionId(otpVerification.getSessionId())
                            .identifierType(IdentifierType.MOBILE.name())
                            .identifierValue(user.getPhone())
                            .action(OtpAttempt.Action.OTP_SUBMIT_RECOVER_CM_ID)
                            .cmId(user.getIdentifier());
                    return validateAndVerifyOtp(otpVerification, builder.build())
                            .then(Mono.just(RecoverCmIdResponse.builder().cmId(user.getIdentifier()).build()));
                });
    }

    public Mono<Session> create(CoreSignUpRequest coreSignUpRequest, String sessionId) {
        UserCredential credential = new UserCredential(coreSignUpRequest.getPassword());
        KeycloakUser keycloakUser = new KeycloakUser(
                coreSignUpRequest.getName(),
                coreSignUpRequest.getUsername(),
                Collections.singletonList(credential),
                Boolean.TRUE.toString());

        return signupService.getMobileNumber(sessionId)
                .switchIfEmpty(Mono.error(new InvalidRequestException("mobile number not verified")))
                .flatMap(mobileNumber -> userExistsWith(coreSignUpRequest.getUsername())
                        .switchIfEmpty(Mono.defer(() -> createUserWith(mobileNumber, coreSignUpRequest, keycloakUser)))
                        .cast(Session.class));
    }

    public Mono<Session> update(UpdateUserRequest updateUserRequest, String sessionId) {
        return signupService.getUserName(sessionId)
                .switchIfEmpty(Mono.error(new InvalidRequestException("user not verified")))
                .flatMap(userName -> updatedSessionFor(updateUserRequest.getPassword(), userName));
    }

    public Mono<Session> updatePassword(UpdatePasswordRequest request, String userName) {
        return tokenService.tokenForUser(userName, request.getOldPassword())
                .onErrorResume(error -> Mono.error(ClientError.unAuthorizedRequest("Invalid old password")))
                .flatMap(session -> updatedSessionFor(request.getNewPassword(), userName));
    }

    private Mono<Session> updatedSessionFor(String password, String userName) {
        return tokenService.tokenForAdmin()
                .flatMap(adminSession -> {
                    return identityServiceClient.getUser(userName, String.format("Bearer %s", adminSession.getAccessToken()))
                            .flatMap(cloakUsers -> identityServiceClient.updateUser(adminSession, cloakUsers.getId(),
                                    password)).then();
                })
                .doOnError(error -> Mono.error(ClientError.failedToUpdateUser()))
                .then(tokenService.tokenForUser(userName, password));
    }

    public Mono<LoginModeResponse> getLoginMode(String userName) {
        return tokenService.tokenForAdmin()
                .flatMap(adminSession -> {
                    String accessToken = format("Bearer %s", adminSession.getAccessToken());
                    return identityServiceClient.getUser(userName, accessToken)
                            .switchIfEmpty(Mono.error(userNotFound()))
                            .flatMap(cloakUsers -> identityServiceClient.getCredentials(cloakUsers.getId(), accessToken))
                            .doOnError(error -> Mono.error(failedToFetchUserCredentials()))
                            .collectList()
                            .flatMap(userCreds -> {
                                if (userCreds.isEmpty())
                                    return Mono.just(LoginModeResponse.builder().loginMode(LoginMode.OTP).build());
                                else
                                    return Mono.just(LoginModeResponse.builder().loginMode(LoginMode.CREDENTIAL).build());
                            });
                });
    }

    private Mono<Object> userExistsWith(String username) {
        return userRepository.userWith(username)
                .flatMap(patient -> {
                    logger.error(format("Patient with %s already exists", patient.getIdentifier()));
                    return Mono.error(userAlreadyExists(patient.getIdentifier()));
                });
    }


    private Mono<Session> createUserWith(String mobileNumber, CoreSignUpRequest coreSignUpRequest, KeycloakUser keycloakUser) {
        User user = User.from(coreSignUpRequest, mobileNumber);
        return userRepository.save(user)
                .then(tokenService.tokenForAdmin()
                        .flatMap(accessToken -> identityServiceClient.createUser(accessToken, keycloakUser))
                        .then())
                .onErrorResume(ClientError.class, error -> {
                    logger.error(error.getMessage(), error);
                    return userRepository.delete(user.getIdentifier()).then();
                })
                .then(tokenService.tokenForUser(coreSignUpRequest.getUsername(), coreSignUpRequest.getPassword()));
    }

    private boolean validateOtpVerification(OtpVerification otpVerification) {
        return otpVerification.getSessionId() != null &&
                !otpVerification.getSessionId().isEmpty() &&
                otpVerification.getValue() != null &&
                !otpVerification.getValue().isEmpty();
    }

    public String getUserIdSuffix() {
        return userServiceProperties.getUserIdSuffix();
    }

    public int getExpiryInMinutes() {
        return otpServiceProperties.getExpiryInMinutes();
    }

    public Mono<User> getPatientByDetails(InitiateCmIdRecoveryRequest request) {
        return userRepository.getUserBy(request.getGender(),
                                IdentifierUtils.getIdentifierValue(
                                        request.getVerifiedIdentifiers(),
                                        IdentifierType.MOBILE))
                .flatMap(users -> new NameFilter().filter(users, request.getName()))
                .flatMap(users -> new YOBFilter().filter(users, request.getYearOfBirth()))
                .flatMap(users -> new ABPMJAYIdFilter().filter(users, request.getUnverifiedIdentifiers()))
                .flatMap(this::getDistinctUser);
    }

    private Mono<User> getDistinctUser(List<User> rows) {
        return rows.size() == 1 ? Mono.just(rows.get(0)) : Mono.empty();
    }
}
