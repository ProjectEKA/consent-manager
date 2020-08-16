package in.projecteka.library.common;

import in.projecteka.library.clients.model.Error;
import in.projecteka.library.clients.model.ErrorRepresentation;
import in.projecteka.library.clients.model.ErrorCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public class DbOperationError extends Throwable {
    private final HttpStatus httpStatus;
    private final ErrorRepresentation error;
    private final String errorMessage = "Failed to persist in database";

    public DbOperationError() {
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = new ErrorRepresentation(new Error(ErrorCode.DB_OPERATION_FAILED, errorMessage));
    }

    public DbOperationError(String errorMessage) {
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = new ErrorRepresentation(new Error(ErrorCode.DB_OPERATION_FAILED, errorMessage));
    }
}
