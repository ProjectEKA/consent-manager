package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.KeycloakToken;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.User;
import in.projecteka.consentmanager.user.model.UserCredential;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private OtpServiceProperties otpServiceProperties;
    private OtpServiceClient otpServiceClient;
    private UserVerificationService userVerificationService;
    private IdentityServiceClient identityServiceClient;
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

    public Mono<KeycloakToken> create(SignUpRequest signUpRequest, String sessionId) {
        if (!isValid(signUpRequest)) {
            throw new InvalidRequestException("invalid.request.body");
        }
        UserCredential credential = new UserCredential(signUpRequest.getPassword());
        KeycloakUser user = new KeycloakUser(
                signUpRequest.getFirstName(),
                signUpRequest.getLastName(),
                signUpRequest.getUserName(),
                Collections.singletonList(credential),
                Boolean.TRUE.toString());

        return userVerificationService.getMobileNumber(sessionId)
                .map(mobileNumber -> tokenService.tokenForAdmin()
                        .flatMap(accessToken -> identityServiceClient.createUser(accessToken, user))
                        .then(userRepository.save(User.from(signUpRequest, mobileNumber)))
                        .then(tokenService.tokenForUser(signUpRequest.getUserName(), signUpRequest.getPassword()))
                        .map(keycloakToken -> keycloakToken))
                .orElse(Mono.error(new InvalidRequestException("mobile number not verified")));
    }

    private boolean validateOtpVerification(OtpVerification otpVerification) {
        return otpVerification.getSessionId() != null &&
                !otpVerification.getSessionId().isEmpty() &&
                otpVerification.getValue() != null &&
                !otpVerification.getValue().isEmpty();
    }

    private String removeCountryCodeFrom(String mobileNumber) {
        var countryCodeSeparator = "-";
        return mobileNumber.split(countryCodeSeparator).length > 1
               ? mobileNumber.split(countryCodeSeparator)[1]
               : mobileNumber;
    }

    private boolean isValid(SignUpRequest signUpRequest) {
        return !StringUtils.isEmpty(signUpRequest.getFirstName()) &&
                !StringUtils.isEmpty(signUpRequest.getUserName()) &&
                !StringUtils.isEmpty(signUpRequest.getPassword()) &&
                signUpRequest.getDateOfBirth().isBefore(LocalDate.now());
    }
}
