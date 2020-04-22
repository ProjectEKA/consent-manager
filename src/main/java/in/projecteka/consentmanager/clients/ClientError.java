package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_ARTEFACT_EXPIRED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_ARTEFACT_FORBIDDEN;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_ARTEFACT_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_NOT_GRANTED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_REQUEST_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_DATE_RANGE;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_PROVIDER_OR_CARE_CONTEXT;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_REQUESTER;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_TOKEN;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_TRANSACTION_PIN;
import static in.projecteka.consentmanager.clients.model.ErrorCode.NETWORK_SERVICE_ERROR;
import static in.projecteka.consentmanager.clients.model.ErrorCode.OTP_EXPIRED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.OTP_INVALID;
import static in.projecteka.consentmanager.clients.model.ErrorCode.PROVIDER_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.QUEUE_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.TRANSACTION_ID_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.TRANSACTION_PIN_IS_ALREADY_CREATED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.UNABLE_TO_CONNECT_TO_PROVIDER;
import static in.projecteka.consentmanager.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USERNAME_OR_PASSWORD_INCORRECT;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USER_ALREADY_EXISTS;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USER_NOT_FOUND;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Getter
@ToString
public class ClientError extends Throwable {

    private static final String CANNOT_PROCESS_REQUEST_TRY_LATER = "Cannot process the request at the moment, please try later.";
    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;

    public ClientError(HttpStatus httpStatus, ErrorRepresentation errorRepresentation) {
        this.httpStatus = httpStatus;
        error = errorRepresentation;
    }

    public static ClientError unableToConnectToProvider() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(UNABLE_TO_CONNECT_TO_PROVIDER, CANNOT_PROCESS_REQUEST_TRY_LATER)));
    }

    public static ClientError userNotFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(USER_NOT_FOUND, "Cannot find the user")));
    }

    public static ClientError transactionPinDidNotMatch() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_TRANSACTION_PIN, "Invalid transaction pin")));
    }

    public static ClientError invalidTransactionPin() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_TRANSACTION_PIN, "Invalid transaction pin")));
    }

    public static ClientError otpExpired() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(OTP_EXPIRED, "OTP Expired, please try again")));
    }

    public static ClientError networkServiceCallFailed() {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(NETWORK_SERVICE_ERROR, CANNOT_PROCESS_REQUEST_TRY_LATER)));
    }

    public static ClientError providerNotFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(PROVIDER_NOT_FOUND, "Cannot find the provider")));
    }

    public static ClientError consentRequestNotFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(CONSENT_REQUEST_NOT_FOUND, "Cannot find the consent request")));
    }

    public static ClientError transactionIdNotFound() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(TRANSACTION_ID_NOT_FOUND, "Failed to get transaction Id")));
    }

    public static ClientError expiryNotFound() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(TRANSACTION_ID_NOT_FOUND,
                        "Failed to get expiry for link reference number")));
    }

    public static ClientError consentArtefactNotFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(CONSENT_ARTEFACT_NOT_FOUND, "Cannot find the consent artefact")));
    }

    public static ClientError consentArtefactForbidden() {
        return new ClientError(FORBIDDEN,
                new ErrorRepresentation(new Error(CONSENT_ARTEFACT_FORBIDDEN, "Cannot retrieve Consent artefact")));
    }

    public static ClientError invalidOtp() {
        return new ClientError(UNAUTHORIZED, new ErrorRepresentation(new Error(OTP_INVALID, "Invalid OTP")));
    }

    public static ClientError unknownErrorOccurred() {
        return internalServerError("Unknown error occurred");
    }

    public static ClientError failedToCreateTransactionPin() {
        return internalServerError("Failed to create transaction pin");
    }

    public static ClientError failedToFetchTransactionPin() {
        return internalServerError("Failed to fetch transaction pin");
    }

    public static ClientError queueNotFound() {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(QUEUE_NOT_FOUND, "Queue not found")));
    }

    public static ClientError invalidRequester() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_REQUESTER, "Not a valid Requester")));
    }

    public static ClientError invalidDateRange() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_DATE_RANGE, "Date Range given is invalid")));
    }

    public static ClientError consentExpired() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(CONSENT_ARTEFACT_EXPIRED, "Consent artefact expired")));
    }

    public static ClientError unAuthorized() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_TOKEN, "Token verification failed")));
    }

    public static ClientError unAuthorizedRequest() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(USERNAME_OR_PASSWORD_INCORRECT,
                        "Username or password is incorrect")));
    }

    public static ClientError userAlreadyExists(String username) {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(USER_ALREADY_EXISTS, format("%s is already exists", username))));
    }

    public static ClientError transactionPinAlreadyCreated() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(TRANSACTION_PIN_IS_ALREADY_CREATED,
                        "Transaction pin is already created")));
    }

    public static ClientError transactionPinNotFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(TRANSACTION_PIN_IS_ALREADY_CREATED, "Transaction pin not found")));
    }

    private static ClientError internalServerError(String message) {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, message)));
    }

    public static ClientError invalidProviderOrCareContext() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_PROVIDER_OR_CARE_CONTEXT,
                        "Invalid Provider or Care Context.")));
    }

    public static ClientError consentNotGranted() {
        return new ClientError(CONFLICT,
                new ErrorRepresentation(new Error(CONSENT_NOT_GRANTED, "Not a granted consent.")));
    }

    public static ClientError invalidAccessToken() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_TOKEN, "Expected token of the format `Bearer accessToken`")));
    }
}
