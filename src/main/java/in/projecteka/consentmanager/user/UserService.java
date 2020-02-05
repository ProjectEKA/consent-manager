package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.exception.InvalidRequestException;
import in.projecteka.consentmanager.user.model.*;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private OtpServiceProperties otpServiceProperties;
    private OtpServiceClient otpServiceClient;

    public Mono<User> userWith(String userName) {
        return userRepository.userWith(userName);
    }

    public Mono<TemporarySession> sendOtp(DeviceIdentifier deviceIdentifier) {
        String deviceType = deviceIdentifier.getIdentifierType().toUpperCase();

        if (!otpServiceProperties.getIdentifiers().contains(deviceType)) {
            throw new InvalidRequestException("invalid.identifier.type");
        }

        String temporarySession = UUID.randomUUID().toString();
        OtpRequest otpRequest = new OtpRequest(temporarySession,
                new OtpCommunicationData(deviceIdentifier.getIdentifierType(), deviceIdentifier.getIdentifier()));

        return otpServiceClient.sendOtpTo(otpRequest);
    }

    public Mono<Token> permitOtp(OtpVerification otpVerification) {
        if (!validateOtpVerification(otpVerification)) {
            throw new InvalidRequestException("invalid.request.body");
        }

        otpServiceClient.permitOtp(otpVerification);

        return Mono.just(new Token());
    }

    private boolean validateOtpVerification(OtpVerification otpVerification) {
        return null != otpVerification.getSessionId() && !otpVerification.getSessionId().isEmpty() &&
                null != otpVerification.getValue() && !otpVerification.getValue().isEmpty();
    }
}
