package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.KeycloakClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.*;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;

@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private OtpServiceProperties otpServiceProperties;
    private OtpServiceClient otpServiceClient;
    private UserVerificationService userVerificationService;
    private KeycloakClient keycloakClient;
    private TokenService tokenService;


    public Mono<User> userWith(String userName) {
        return userRepository.userWith(userName);
    }

    public Mono<SignUpSession> sendOtp(UserSignUpEnquiry userSignupEnquiry) {
        String identifierType = userSignupEnquiry.getIdentifierType().toUpperCase();

        if (!otpServiceProperties.getIdentifiers().contains(identifierType)) {
            throw new InvalidRequestException("invalid.identifier.type");
        }

        String sessionId = UUID.randomUUID().toString();
        OtpRequest otpRequest = new OtpRequest(
                sessionId,
                new OtpCommunicationData(userSignupEnquiry.getIdentifierType(),
                        removeCountryCodeFrom(userSignupEnquiry.getIdentifier())));

        return otpServiceClient
                .send(otpRequest)
                .thenReturn(userVerificationService.cacheAndSendSession(
                        otpRequest.getSessionId(),
                        otpRequest.getCommunication().getValue()));
    }

    public Mono<Token> permitOtp(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            throw new InvalidRequestException("invalid.request.body");
        }

        return otpServiceClient
                .verify(otpVerification.getSessionId(), otpVerification.getValue())
                .thenReturn(userVerificationService.generateToken(otpVerification.getSessionId()));
    }

    public Mono<KeycloakToken> create(SignUpRequest signUpRequest) {
        if(!validateUserSignUp(signUpRequest)) {
            throw new InvalidRequestException("invalid.request.body");
        }
        UserCredential credential = new UserCredential(signUpRequest.getPassword());
        KeycloakCreateUserRequest createUserRequest =
                new KeycloakCreateUserRequest(
                        signUpRequest.getFirstName(),
                        signUpRequest.getLastName(),
                        signUpRequest.getUserName(),
                        Arrays.asList(credential),
                        "true");


        Mono<KeycloakToken> token = tokenService.tokenForAdmin();
        return token
                .flatMap(accessToken -> keycloakClient.createUser(accessToken, createUserRequest))
                .then(tokenService.tokenForUser(signUpRequest.getUserName(), signUpRequest.getPassword()))
                .map(keycloakToken -> keycloakToken);
    }

    private boolean validateOtpVerification(OtpVerification otpVerification) {
        return otpVerification.getSessionId() != null &&
                !otpVerification.getSessionId().isEmpty() &&
                otpVerification.getValue() != null &&
                !otpVerification.getValue().isEmpty();
    }

    private String removeCountryCodeFrom(String mobileNumber) {
        return mobileNumber.split("-").length > 1
                ? mobileNumber.split("-")[1]
                : mobileNumber;
    }

    private boolean validateUserSignUp(SignUpRequest signUpRequest) {
        return !StringUtils.isEmpty(signUpRequest.getFirstName()) &&
                !StringUtils.isEmpty(signUpRequest.getLastName()) &&
                !StringUtils.isEmpty(signUpRequest.getUserName()) &&
                !StringUtils.isEmpty(signUpRequest.getPassword());
    }
}
