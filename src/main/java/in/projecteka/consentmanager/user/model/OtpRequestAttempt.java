package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class OtpRequestAttempt {
    private final String identifierType;
    private final String identifierValue;
    private final AttemptStatus attemptStatus;
    private final LocalDateTime attemptAt;
    private final Action action;
    private final String cmId;

    public enum Action {
        REGISTRATION,
        LOGIN,
        RECOVER_PASSWORD;

        public static Action from(String name) {
            return Action.valueOf(name.replace("OTP_REQUEST_", ""));
        }

        @Override
        public String toString() {
            return "OTP_REQUEST_" + this.name();
        }
    }

    public enum AttemptStatus {
        SUCCESS,
        FAILURE
    }
}
