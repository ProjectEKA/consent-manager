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
import in.projecteka.consentmanager.user.model.IdentifierGroup;
import in.projecteka.consentmanager.user.model.CoreSignUpRequest;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.LoginMode;
import in.projecteka.consentmanager.user.model.LoginModeResponse;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.RecoverCmIdRequest;
import in.projecteka.consentmanager.user.model.RecoverCmIdResponse;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdatePasswordRequest;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserCredential;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.IdentifierType;
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

    public Mono<SignUpSession> sendOtpForPasswordChange(UserSignUpEnquiry userSignupEnquiry, String userName) {
        String identifierType = userSignupEnquiry.getIdentifierType().toUpperCase();

        if (!otpServiceProperties.getIdentifiers().contains(identifierType)) {
            throw new InvalidRequestException("invalid.identifier.type");
        }

        String sessionId = UUID.randomUUID().toString();
        OtpRequest otpRequest = new OtpRequest(
                sessionId,
                new OtpCommunicationData(userSignupEnquiry.getIdentifierType(), userSignupEnquiry.getIdentifier()));

        return otpAttemptService.validateOTPRequest(identifierType, userSignupEnquiry.getIdentifier(), OtpAttempt.Action.OTP_REQUEST_RECOVER_PASSWORD, userName)
                .then(otpServiceClient
                        .send(otpRequest)
                        .then(signupService.updatedVerfiedSession(
                                otpRequest.getSessionId(),
                                userName)));
    }

    private Mono<Void> validateAndVerifyOtp(OtpVerification otpVerification, OtpAttempt attempt){
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
        return signupService.getUserName(otpVerification.getSessionId())
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(userRepository::userWith)
                .flatMap(user -> {
                    OtpAttempt.OtpAttemptBuilder builder = OtpAttempt.builder()
                            .sessionId(otpVerification.getSessionId())
                            .identifierType(IdentifierType.MOBILE.name())
                            .identifierValue(user.getPhone())
                            .action(OtpAttempt.Action.OTP_SUBMIT_RECOVER_PASSWORD)
                            .cmId(user.getIdentifier());
                    return validateAndVerifyOtp(otpVerification, builder.build())
                            .then(signupService.generateToken(new HashMap<>(), otpVerification.getSessionId()));
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
        return tokenService.tokenForUser( userName, request.getOldPassword())
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

    public Mono<RecoverCmIdResponse> recoverCmId(RecoverCmIdRequest request) {
        return isInvalidRecoveryRequest(request)
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.invalidRecoveryRequest())))
                .flatMap(validRequest -> userRepository.getCmIdBy(validRequest.getGender(), ABPMJAYIdFilter.getIdentifierValue(validRequest.getVerifiedIdentifiers(), IdentifierType.MOBILE)))
                .flatMap(rows-> new NameFilter().filter(rows,request.getName()))
                .flatMap(rows -> new YOBFilter().filter(rows, request.getYearOfBirth()))
                .flatMap(rows -> new ABPMJAYIdFilter().filter(rows, request.getUnverifiedIdentifiers()))
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.noPatientFound())))
                .flatMap(this::validateNonZeroRows)
                .flatMap(row -> Mono.just(RecoverCmIdResponse.builder().cmId(row.getIdentifier()).build()))
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.multiplePatientsFound())));
    }

    private boolean isInvalidIdentifierMapped(List<Identifier> identifiers, IdentifierGroup identifierGroup) {
        return identifiers.stream().anyMatch(identifier -> !identifier.getType().getIdentifierGroup().equals(identifierGroup) || !identifier.getType().isValid(identifier.getValue()));
    }

    private Mono<RecoverCmIdRequest> isInvalidRecoveryRequest(RecoverCmIdRequest request) { //breakdown
        boolean areMandatoryFieldsNull = request.getName() == null || request.getGender() == null || !ABPMJAYIdFilter.isIdentifierTypePresent(request.getVerifiedIdentifiers(), IdentifierType.MOBILE);
        boolean isInvalidVerifiedIdentifierMapped = isInvalidIdentifierMapped(request.getVerifiedIdentifiers(), IdentifierGroup.VERIFIED_IDENTIFIER);
        boolean isInvalidUnverifiedIdentifierMapped = isInvalidIdentifierMapped(request.getUnverifiedIdentifiers(), IdentifierGroup.UNVERIFIED_IDENTIFIER);
        return areMandatoryFieldsNull || isInvalidVerifiedIdentifierMapped || isInvalidUnverifiedIdentifierMapped ? Mono.empty() : Mono.just(request);
    }

    private Mono<User> validateNonZeroRows(List<User> rows) {
        return rows.size() == 1 ? Mono.just(rows.get(0)) : Mono.empty();
    }
}
