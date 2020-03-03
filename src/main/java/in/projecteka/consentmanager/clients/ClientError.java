package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.link.link.model.Error;
import in.projecteka.consentmanager.link.link.model.ErrorCode;
import in.projecteka.consentmanager.link.link.model.ErrorRepresentation;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClientError extends Throwable {

    private static final String CANNOT_PROCESS_REQUEST_TRY_LATER = "Cannot process the request at the moment, please try later.";
    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;

    public ClientError(HttpStatus httpStatus, ErrorRepresentation errorRepresentation) {
        this.httpStatus = httpStatus;
        error = errorRepresentation;
    }

    public static ClientError unableToConnectToProvider() {
        return new ClientError(
                HttpStatus.NOT_FOUND,
                new ErrorRepresentation(new Error(
                        ErrorCode.UNABLE_TO_CONNECT_TO_PROVIDER,
                        CANNOT_PROCESS_REQUEST_TRY_LATER)));
    }

    public static ClientError userNotFound() {
        return new ClientError(
                HttpStatus.NOT_FOUND,
                new ErrorRepresentation(new Error(
                        ErrorCode.USER_NOT_FOUND,
                        "Cannot find the user")));
    }

    public static ClientError dbOperationFailed() {
        return new ClientError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(
                        ErrorCode.DB_OPERATION_FAILED,
                        CANNOT_PROCESS_REQUEST_TRY_LATER)));
    }

    public static ClientError otpExpired() {
        return new ClientError(
                HttpStatus.UNAUTHORIZED,
                new ErrorRepresentation(new Error(
                        ErrorCode.OTP_EXPIRED,
                        "OTP Expired, please try again")));
    }

    public static ClientError networkServiceCallFailed() {
        return new ClientError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(
                        ErrorCode.NETWORK_SERVICE_ERROR,
                        CANNOT_PROCESS_REQUEST_TRY_LATER)));
    }

    public static ClientError providerNotFound() {
        return new ClientError(
                HttpStatus.NOT_FOUND,
                new ErrorRepresentation(new Error(
                        ErrorCode.PROVIDER_NOT_FOUND,
                        "Cannot find the provider")));
    }

    public static ClientError consentRequestNotFound() {
        return new ClientError(
                HttpStatus.NOT_FOUND,
                new ErrorRepresentation(new Error(
                        ErrorCode.CONSENT_REQUEST_NOT_FOUND,
                        "Cannot find the consent request")));
    }

    public static ClientError consentArtefactNotFound() {
        return new ClientError(
                HttpStatus.NOT_FOUND,
                new ErrorRepresentation(new Error(
                        ErrorCode.CONSENT_ARTEFACT_NOT_FOUND,
                        "Cannot find the consent artefact")));
    }

    public static ClientError consentArtefactForbidden() {
        return new ClientError(
                HttpStatus.FORBIDDEN,
                new ErrorRepresentation(new Error(
                        ErrorCode.CONSENT_ARTEFACT_FORBIDDEN,
                        "Cannot retrieve Consent artefact. Forbidden")));
    }

    public static ClientError otpNotFound() {
        return new ClientError(
                HttpStatus.NOT_FOUND,
                new ErrorRepresentation(new Error(ErrorCode.OTP_INVALID,
                        "Invalid OTP")));
    }

    public static ClientError unknownErrorOccurred() {
        return new ClientError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(
                        ErrorCode.UNKNOWN_ERROR_OCCURRED,
                        "Unknown error occurred")));
    }

    public static ClientError queueNotFound() {
        return new ClientError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(
                        ErrorCode.QUEUE_NOT_FOUND,
                        "Queue not found")));
    }

    public static ClientError invalidHIU() {
        return new ClientError(
                HttpStatus.UNAUTHORIZED,
                new ErrorRepresentation(new Error(
                        ErrorCode.INVALID_HIU,
                        "Not a valid HIU")));
    }

    public static ClientError invalidDateRange() {
        return new ClientError(
                HttpStatus.UNAUTHORIZED,
                new ErrorRepresentation(new Error(
                        ErrorCode.INVALID_DATE_RANGE,
                        "Date Range given is invalid")));
    }

    public static ClientError consentExpired() {
        return new ClientError(
                HttpStatus.UNAUTHORIZED,
                new ErrorRepresentation(new Error(
                        ErrorCode.CONSENT_ARTEFACT_EXPIRED,
                        "Consent artefact expired")));
    }

    public static ClientError unAuthorized() {
        return new ClientError(
                HttpStatus.UNAUTHORIZED,
                new ErrorRepresentation(
                        new Error(ErrorCode.INVALID_TOKEN,
                                "Token verification failed")));
    }

    public static ClientError unAuthorizedRequest() {
        return new ClientError(
                HttpStatus.UNAUTHORIZED,
                new ErrorRepresentation(
                        new Error(ErrorCode.USERNAME_OR_PASSWORD_INCORRECT,
                                "Username or password is incorrect")));
    }
}
