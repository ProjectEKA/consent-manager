package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ErrorCode {
    NO_PATIENT_FOUND(1404),
    CARE_CONTEXT_NOT_FOUND(1402),
    OTP_INVALID(1405),
    OTP_EXPIRED(1406),
    UNABLE_TO_CONNECT_TO_PROVIDER(1503),
    USER_NOT_FOUND(1414),
    DB_OPERATION_FAILED(1502),
    PROVIDER_NOT_FOUND(1505),
    NETWORK_SERVICE_ERROR(1511),
    CONSENT_REQUEST_NOT_FOUND(1415),
    CONSENT_ARTEFACT_NOT_FOUND(1416),
    CONSENT_ARTEFACT_FORBIDDEN(1508),
    UNKNOWN_ERROR_OCCURRED(1500),
    QUEUE_NOT_FOUND(1501),
    INVALID_REQUESTER(1417),
    INVALID_DATE_RANGE(1418),
    CONSENT_ARTEFACT_EXPIRED(1410),
    INVALID_TOKEN(1401),
    USERNAME_OR_PASSWORD_INCORRECT(1433),
    USER_ALREADY_EXISTS(1434),
    TRANSACTION_PIN_IS_ALREADY_CREATED(1424),
    INVALID_PROVIDER_OR_CARE_CONTEXT(1421),
    INVALID_TRANSACTION_PIN(1425),
    TRANSACTION_PIN_NOT_FOUND(1509),
    CONSENT_NOT_GRANTED(1428),
    INVALID_STATE(1411),
    TRANSACTION_ID_NOT_FOUND(1427),
    INVALID_SCOPE(1431),
    REQUEST_ALREADY_EXISTS(1412),
    OTP_REQUEST_LIMIT_EXCEEDED(1407),
    INVALID_SESSION(1430),
    USER_TEMPORARILY_BLOCKED(1423),
    INVALID_PIN_ATTEMPTS_EXCEEDED(1429),
    INVALID_HITYPE(1419),
    INVALID_PURPOSE(1420),
    INVALID_OTP_ATTEMPTS_EXCEEDED(1408),
    NO_RESULT_FROM_GATEWAY(1504),
    BAD_REQUEST_FROM_GATEWAY(1510),
    INVALID_RECOVERY_REQUEST(1432),
    INVALID_LINK_REFERENCE(1413),
    FAILED_TO_GET_LINKED_CARE_CONTEXTS(1507),
    DUPLICATE_DISCOVERY_REQUEST(1409),
    UNPROCESSABLE_RESPONSE_FROM_GATEWAY(1422),
    REFRESH_TOKEN_INCORRECT(1403),
    INVALID_RESP_REQUEST_ID(1506),
    SERVICE_DOWN(1503),
    DUPLICATE_DISCOVERY_REQUEST(1409); /*please resume codes from the line above, we will put the codes in order later
    and in ranges*/

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
