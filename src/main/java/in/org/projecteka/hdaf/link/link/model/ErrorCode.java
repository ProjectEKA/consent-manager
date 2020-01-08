package in.org.projecteka.hdaf.link.link.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter(onMethod_ = @JsonValue)
public enum ErrorCode {
    NoPatientFound(1000),
    MultiplePatientsFound(1001),
    CareContextNotFound(1002),
    OtpInValid(1003),
    OtpExpired(1004);

    private int value;
    ErrorCode(int val) {
        value = val;
    }

    @JsonCreator
    public static ErrorCode getNameByValue(int value) {
        return Arrays.stream(ErrorCode.values())
                .filter(errorCode -> errorCode.value == value)
                .findAny()
                .orElse(ErrorCode.OtpExpired);
    }
}




