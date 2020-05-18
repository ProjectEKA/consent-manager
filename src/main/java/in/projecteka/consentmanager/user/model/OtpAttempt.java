package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@Getter
public class OtpAttempt {
    @Builder.Default
    private String sessionId = "";
    @Builder.Default
    private String cmId = "";

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
        OTP_SUBMIT_REGISTRATION,
        OTP_SUBMIT_RECOVER_PASSWORD,
        OTP_SUBMIT_LOGIN;
    }

    public enum AttemptStatus {
        SUCCESS,
        FAILURE
    }
}
