package in.projecteka.user.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@Getter
public class OtpAttempt {
    @Builder.Default
    private final String sessionId = "";
    @Builder.Default
    private final String cmId = "";

    private final String identifierType;
    private final String identifierValue;
    private final AttemptStatus attemptStatus;
    private final LocalDateTime attemptAt;
    private final Action action;

    public String getIdentifierType(){
        return this.identifierType.toUpperCase();
    }

    public enum Action {
        OTP_REQUEST_REGISTRATION,
        OTP_REQUEST_LOGIN,
        OTP_REQUEST_RECOVER_PASSWORD,
        OTP_REQUEST_RECOVER_CM_ID,
        OTP_REQUEST_FORGOT_CONSENT_PIN,
        OTP_SUBMIT_REGISTRATION,
        OTP_SUBMIT_RECOVER_PASSWORD,
        OTP_SUBMIT_LOGIN,
        OTP_SUBMIT_FORGOT_CONSENT_PIN,
        OTP_SUBMIT_RECOVER_CM_ID;
    }

    public enum AttemptStatus {
        SUCCESS,
        FAILURE
    }
}
