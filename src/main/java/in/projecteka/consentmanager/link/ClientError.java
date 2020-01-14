package in.projecteka.consentmanager.link;

import in.projecteka.consentmanager.link.link.model.Error;
import in.projecteka.consentmanager.link.link.model.ErrorCode;
import in.projecteka.consentmanager.link.link.model.ErrorRepresentation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClientError extends Throwable {

    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;

    public ClientError(HttpStatus httpStatus, ErrorRepresentation errorRepresentation) {
        this.httpStatus = httpStatus;
        error = errorRepresentation;
    }

    public static ClientError unableToConnectToProvider(){
        return new ClientError(
                HttpStatus.NOT_FOUND,
                new ErrorRepresentation(new Error(
                        ErrorCode.UnableToConnectToProvider,
                        "Cannot link at the moment, please try later.")));
    }

    public static ClientError otpExpired() {
        return new ClientError(
                HttpStatus.UNAUTHORIZED,
                new ErrorRepresentation(new Error(
                        ErrorCode.OtpExpired,
                        "OTP Expired, please try again")));
    }
}
