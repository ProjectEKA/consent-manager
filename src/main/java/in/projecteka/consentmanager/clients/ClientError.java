package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.RespError;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import static in.projecteka.consentmanager.clients.model.ErrorCode.BAD_REQUEST_FROM_GATEWAY;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_ARTEFACT_EXPIRED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_ARTEFACT_FORBIDDEN;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_ARTEFACT_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_NOT_GRANTED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.CONSENT_REQUEST_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_DATE_RANGE;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_OTP_ATTEMPTS_EXCEEDED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_PIN_ATTEMPTS_EXCEEDED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_PROVIDER_OR_CARE_CONTEXT;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_RECOVERY_REQUEST;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_REQUESTER;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_RESP_REQUEST_ID;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_SCOPE;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_SESSION;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_TOKEN;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_TRANSACTION_PIN;
import static in.projecteka.consentmanager.clients.model.ErrorCode.NETWORK_SERVICE_ERROR;
import static in.projecteka.consentmanager.clients.model.ErrorCode.NO_PATIENT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.NO_RESULT_FROM_GATEWAY;
import static in.projecteka.consentmanager.clients.model.ErrorCode.OTP_EXPIRED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.OTP_INVALID;
import static in.projecteka.consentmanager.clients.model.ErrorCode.OTP_REQUEST_LIMIT_EXCEEDED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.PROVIDER_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.REFRESH_TOKEN_INCORRECT;
import static in.projecteka.consentmanager.clients.model.ErrorCode.QUEUE_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.REQUEST_ALREADY_EXISTS;
import static in.projecteka.consentmanager.clients.model.ErrorCode.TRANSACTION_ID_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.TRANSACTION_PIN_IS_ALREADY_CREATED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.UNABLE_TO_CONNECT_TO_PROVIDER;
import static in.projecteka.consentmanager.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.UNPROCESSABLE_RESPONSE_FROM_GATEWAY;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USERNAME_OR_PASSWORD_INCORRECT;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USER_ALREADY_EXISTS;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USER_NOT_FOUND;
import static in.projecteka.consentmanager.clients.model.ErrorCode.USER_TEMPORARILY_BLOCKED;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_REQUEST;
import static in.projecteka.consentmanager.clients.model.ErrorCode.TRANSACTION_PIN_NOT_FOUND;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.FORBIDDEN;


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

    public static ClientError tooManyRequests() {
        return new ClientError(TOO_MANY_REQUESTS, new ErrorRepresentation(
                new Error(INVALID_REQUEST, "Too many requests from gateway")));
    }

    public ErrorCode getErrorCode(){
        return this.error.getError().getCode();
    }

    public static ClientError unableToConnectToProvider() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(UNABLE_TO_CONNECT_TO_PROVIDER, CANNOT_PROCESS_REQUEST_TRY_LATER)));
    }

    public static ClientError userNotFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(USER_NOT_FOUND, "Cannot find the user")));
    }

    public static ClientError transactionPinDidNotMatch(String auxMessage) {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_TRANSACTION_PIN, String.format("%s;%s","Invalid transaction pin",auxMessage))));
    }

    public static ClientError invalidAttemptsExceeded() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_PIN_ATTEMPTS_EXCEEDED, "Invalid Pin attempts exceeded; Try again after sometime.")));
    }

    public static ClientError invalidTransactionPin() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_TRANSACTION_PIN, "Invalid transaction pin")));
    }

    public static ClientError otpExpired() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(OTP_EXPIRED, "OTP Expired, please try again")));
    }

    public static ClientError otpRequestLimitExceeded() {
        return new ClientError(TOO_MANY_REQUESTS,
                new ErrorRepresentation(new Error(OTP_REQUEST_LIMIT_EXCEEDED, "OTP request limit exceeded")));
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

    public static ClientError failedToGenerateOtp() {
        return internalServerError("Failed to generate otp");
    }

    public static ClientError failedToCreateTransactionPin() {
        return internalServerError("Failed to create transaction pin");
    }


    public static ClientError failedToInsertLockedUser() {
        return internalServerError("Failed to insert locked user");
    }


    public static ClientError failedToUpdateLockedUser() {
        return internalServerError("Failed to update locked user");
    }

    public static ClientError failedToUpdateTransactionPin() {
        return internalServerError("Failed to update request_id in transaction pin");
    }

    public static ClientError failedToEditTransactionPin() {
        return internalServerError("Failed to update transaction pin");
    }

    public static ClientError failedToFetchTransactionPin() {
        return internalServerError("Failed to fetch transaction pin");
    }


    public static ClientError failedToFetchLockedUser() {
        return internalServerError("Failed to fetch Locked User");
    }

    public static ClientError failedToUpdateUser() {
        return internalServerError("Failed to update user");
    }

    public static ClientError failedToFetchUserCredentials() {
        return internalServerError("Failed to get user credentials");
    }

    public static ClientError queueNotFound() {
        return new ClientError(INTERNAL_SERVER_ERROR,
                new ErrorRepresentation(new Error(QUEUE_NOT_FOUND, "Queue not found")));
    }

    public static ClientError invalidRequester() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_REQUESTER, "Not a valid Requester")));
    }

    public static ClientError invalidRequester(String errorMessage) {
        return new ClientError(BAD_REQUEST, new ErrorRepresentation(new Error(INVALID_REQUESTER, errorMessage)));
    }

    public static ClientError invalidDateRange() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_DATE_RANGE, "Date Range given is invalid")));
    }

    public static ClientError consentExpired() {
        return new ClientError(HttpStatus.GONE,
                new ErrorRepresentation(new Error(CONSENT_ARTEFACT_EXPIRED, "Consent artefact expired")));
    }

    public static ClientError unAuthorized() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_TOKEN, "Token verification failed")));
    }

    public static ClientError unAuthorizedRequest(String errorMessage) {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(USERNAME_OR_PASSWORD_INCORRECT,
                        errorMessage)));
    }

    public static ClientError invalidUserNameOrPassword() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(USERNAME_OR_PASSWORD_INCORRECT,
                        "Invalid username or password")));
    }

    public static ClientError invalidRefreshToken() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(REFRESH_TOKEN_INCORRECT,
                        "Invalid refresh token")));
    }

    public static ClientError userBlocked() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(USER_TEMPORARILY_BLOCKED,
                        "User blocked temporarily")));
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
                new ErrorRepresentation(new Error(TRANSACTION_PIN_NOT_FOUND, "Transaction pin not found")));
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
        return new ClientError(PRECONDITION_FAILED,
                new ErrorRepresentation(new Error(CONSENT_NOT_GRANTED, "Not a granted consent.")));
    }

    public static ClientError invalidAccessToken() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_TOKEN, "Expected token of the format `Bearer accessToken`")));
    }

    public static ClientError requestAlreadyExists() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(REQUEST_ALREADY_EXISTS,
                        "A request with this request id already exists.")));
    }

    public static ClientError invalidScope() {
        return new ClientError(UNAUTHORIZED,
                new ErrorRepresentation(new Error(INVALID_SCOPE, "The scope provided is invalid for current operation")));
    }

    public static ClientError invalidSession(String session) {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_SESSION, String.format("The sessionId: %s is invalid",session))));
    }

    public static ClientError gatewayTimeOut() {
        return new ClientError(GATEWAY_TIMEOUT, new ErrorRepresentation(new Error(NO_RESULT_FROM_GATEWAY, "Didn't receive any result from Gateway")));
    }

    public static ClientError tooManyInvalidOtpAttempts() {
        return new ClientError(TOO_MANY_REQUESTS,
                new ErrorRepresentation(new Error(INVALID_OTP_ATTEMPTS_EXCEEDED, "Invalid OTP attempts limit exceeded")));
    }

    public static ClientError unknownUnauthroziedError(String message) {
        return new ClientError(UNAUTHORIZED, new ErrorRepresentation(new Error(UNKNOWN_ERROR_OCCURRED, message)));
    }

    public static ClientError patientNotFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(NO_PATIENT_FOUND, "Could not find patient information")));
    }

    public static ClientError unprocessableEntity() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(BAD_REQUEST_FROM_GATEWAY, "Bad Request")));

    }

    public static ClientError invalidRecoveryRequest() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_RECOVERY_REQUEST, "Invalid CM Id recovery request")));
    }

    public static ClientError noPatientFound() {
        return new ClientError(NOT_FOUND,
                new ErrorRepresentation(new Error(NO_PATIENT_FOUND, "No patient matching the records")));
    }

    public static ClientError invalidResponseFromHIP() {
        return new ClientError(UNPROCESSABLE_ENTITY, new ErrorRepresentation(new Error(UNPROCESSABLE_RESPONSE_FROM_GATEWAY, "Could not process response from HIP")));
    }

    public static ClientError invalidOldPassword(int remainingAttempts) {
        return new ClientError(UNAUTHORIZED, new ErrorRepresentation(new Error(USERNAME_OR_PASSWORD_INCORRECT, "You have " + remainingAttempts + " tries available after that your account will be locked")));
    }

    public static RespError from(ClientError exception) {
        return RespError.builder().code(exception.getErrorCode().getValue()).message(exception.getMessage()).build();
    }

    public static ClientError invalidResponseFromGateway() {
        return new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_RESP_REQUEST_ID, "resp.requestId is null or empty")));
    }
}
