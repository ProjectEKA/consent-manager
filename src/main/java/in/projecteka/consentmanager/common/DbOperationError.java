package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import static in.projecteka.consentmanager.clients.model.ErrorCode.DB_OPERATION_FAILED;

@Getter
@ToString
public class DbOperationError extends Throwable {
    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;
    private final String errorMessage = "Failed to persist in database";

    public DbOperationError() {
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = new ErrorRepresentation(new Error(DB_OPERATION_FAILED, errorMessage));
    }
}
