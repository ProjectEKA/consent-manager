package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.*;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.ClientError.*;
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
    private final OtpRequestAttemptService otpRequestAttemptService;

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

        return otpRequestAttemptService
                .validateOTPRequest(userSignupEnquiry.getIdentifierType(), userSignupEnquiry.getIdentifier(), OtpRequestAttempt.Action.REGISTRATION)
                .then(otpServiceClient.send(otpRequest)
                        .then(signupService.cacheAndSendSession(
                                otpRequest.getSessionId(),
                                otpRequest.getCommunication().getValue())));
    }

    public Mono<SignUpSession> sendOtpForPasswordChange(UserSignUpEnquiry userSignupEnquiry, String userName) {
        String identifierType = userSignupEnquiry.getIdentifierType().toUpperCase();

        if (!otpServiceProperties.getIdentifiers().contains(identifierType)) {
            throw new InvalidRequestException("invalid.identifier.type");
        }

        String sessionId = UUID.randomUUID().toString();
        OtpRequest otpRequest = new OtpRequest(
                sessionId,
                new OtpCommunicationData(userSignupEnquiry.getIdentifierType(), userSignupEnquiry.getIdentifier()));

        return otpRequestAttemptService.validateOTPRequest(userSignupEnquiry.getIdentifierType(), userSignupEnquiry.getIdentifier(),OtpRequestAttempt.Action.RECOVER_PASSWORD,userName)
                .then(otpServiceClient
                .send(otpRequest)
                .then(signupService.updatedVerfiedSession(
                        otpRequest.getSessionId(),
                        userName)));
    }

    public Mono<Token> permitOtp(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            throw new InvalidRequestException("invalid.request.body");
        }
        return otpServiceClient
                .verify(otpVerification.getSessionId(), otpVerification.getValue())
                .then(signupService.generateToken(otpVerification.getSessionId()));
    }

    public Mono<Token> verifyOtp(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            throw new InvalidRequestException("invalid.request.body");
        }
        return otpServiceClient
                .verify(otpVerification.getSessionId(), otpVerification.getValue())
                .then(signupService.generateToken(new HashMap<>(), otpVerification.getSessionId()));
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

    private Mono<Session> updatedSessionFor(String password, String userName) {
        return getSession(password, userName, failedToUpdateUser());
    }

    public Mono<Session> updatePasswordFor(UpdatePasswordRequest request, String userName) {
        return tokenService.tokenForUser( userName, request.getOldPassword())
                .onErrorResume(error -> Mono.error(ClientError.unAuthorizedRequest("Invalid old password")))
                .flatMap(session -> getSession(request.getNewPassword(), userName, failedToUpdatePassword()));
    }

    private Mono<Session> getSession(String password, String userName, ClientError clientError) {
        return tokenService.tokenForAdmin()
                .flatMap(adminSession -> {
                    return identityServiceClient.getUser(userName, String.format("Bearer %s", adminSession.getAccessToken()))
                            .flatMap(cloakUsers -> identityServiceClient.updateUser(adminSession, cloakUsers.getId(),
                                    password)).then();
                })
                .doOnError(error -> Mono.error(clientError))
                .then(tokenService.tokenForUser(userName, password));
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
}
