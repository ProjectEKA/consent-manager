package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class OtpAttempt {
    private final String sessionId;
    private final String identifierType;
    private final String identifierValue;
    private final AttemptStatus attemptStatus;
    private final LocalDateTime attemptAt;
    private final Action action;
    private final String cmId;

    public enum Action {
        OTP_REQUEST_REGISTRATION,
        OTP_REQUEST_LOGIN,
        OTP_REQUEST_RECOVER_PASSWORD,
        OTP_SUBMIT_REGISTRATION,
        OTP_SUBMIT_RECOVER_PASSWORD;
    }

    public enum AttemptStatus {
        SUCCESS,
        FAILURE
    }
}
