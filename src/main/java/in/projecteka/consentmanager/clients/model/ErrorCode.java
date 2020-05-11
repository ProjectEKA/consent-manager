package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ErrorCode {
    NO_PATIENT_FOUND(1000),
    MULTIPLE_PATIENTS_FOUND(1001),
    CARE_CONTEXT_NOT_FOUND(1002),
    OTP_INVALID(1003),
    OTP_EXPIRED(1004),
    UNABLE_TO_CONNECT_TO_PROVIDER(1005),
    USER_NOT_FOUND(1006),
    DB_OPERATION_FAILED(1007),
    PROVIDER_NOT_FOUND(1008),
    NETWORK_SERVICE_ERROR(2000),
    CONSENT_REQUEST_NOT_FOUND(1009),
    CONSENT_ARTEFACT_NOT_FOUND(1010),
    CONSENT_ARTEFACT_FORBIDDEN(1011),
    UNKNOWN_ERROR_OCCURRED(1012),
    QUEUE_NOT_FOUND(1013),
    INVALID_REQUESTER(1014),
    INVALID_DATE_RANGE(1015),
    CONSENT_ARTEFACT_EXPIRED(1016),
    INVALID_TOKEN(1017),
    USERNAME_OR_PASSWORD_INCORRECT(1018),
    USER_ALREADY_EXISTS(1019),
    TRANSACTION_PIN_IS_ALREADY_CREATED(1020),
    INVALID_PROVIDER_OR_CARE_CONTEXT(1021),
    INVALID_TRANSACTION_PIN(1022),
    TRANSACTION_PIN_NOT_FOUND(1023),
    CONSENT_NOT_GRANTED(1024),
    INVALID_STATE(1025),
    TRANSACTION_ID_NOT_FOUND(1026),
    INVALID_SCOPE(1027),
    REQUEST_ALREADY_EXISTS(1028),
    OTP_REQUEST_LIMIT_EXCEEDED(1029),
    INVALID_SESSION(1030);

    private final int value;

    ErrorCode(int val) {
        value = val;
    }

    // Adding @JsonValue annotation that tells the 'value' to be of integer type while de-serializing.
    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static ErrorCode getNameByValue(int value) {
        return Arrays.stream(ErrorCode.values())
                .filter(errorCode -> errorCode.value == value)
                .findAny()
                .orElse(ErrorCode.UNKNOWN_ERROR_OCCURRED);
    }
}