package in.org.projecteka.hdaf.link.link.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ErrorCode {
    NoPatientFound(1000),
    MultiplePatientsFound(1001),
    CareContextNotFound(1002),
    OtpInValid(1003),
    OtpExpired(1004),
    HIPNotFound(1005);

    private int value;
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
                .orElse(ErrorCode.OtpExpired);
    }
}