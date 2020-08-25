package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.LogoutRequest;
import in.projecteka.consentmanager.user.model.OtpPermitRequest;
import in.projecteka.consentmanager.user.model.OtpVerificationRequest;
import in.projecteka.consentmanager.user.model.OtpVerificationResponse;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.Session;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@ConditionalOnExpression("${consentmanager.userservice.enabled:true}")
public class SessionController {
    private final SessionService sessionService;

    @PostMapping(Constants.APP_PATH_NEW_SESSION)
    public Mono<Session> forNew(@RequestBody SessionRequest sessionRequest) {
        return sessionService.forNew(sessionRequest);
    }

    @PostMapping(Constants.APP_PATH_VERIFY_OTP_FOR_SESSION)
    public Mono<OtpVerificationResponse> verify(@RequestBody OtpVerificationRequest otpVerificationRequest) {
        return sessionService.sendOtp(otpVerificationRequest);
    }

    @PostMapping(Constants.APP_PATH_SESSION_PERMIT_BY_OTP)
    public Mono<Session> permit(@RequestBody OtpPermitRequest otpPermitRequest) {
        return sessionService.validateOtp(otpPermitRequest);
    }

    @PostMapping(Constants.APP_PATH_LOGOUT)
    public Mono<Void> logout(@RequestHeader(name = "Authorization") String accessToken,
                             @RequestBody LogoutRequest logoutRequest) {
        String[] splitAccessToken = accessToken.split(" ");
        if (splitAccessToken.length != 2) {
            return Mono.error(ClientError.invalidAccessToken());
        }
        return sessionService.logout(splitAccessToken[1], logoutRequest);
    }
}
