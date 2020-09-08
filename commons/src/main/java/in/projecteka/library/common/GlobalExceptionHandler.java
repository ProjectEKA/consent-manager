package in.projecteka.library.common;

import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.Error;
import in.projecteka.library.clients.model.ErrorCode;
import in.projecteka.library.clients.model.ErrorRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.boot.web.error.ErrorAttributeOptions.defaults;

@Slf4j
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {
    public GlobalExceptionHandler(
            ErrorAttributes errorAttributes,
            ResourceProperties resourceProperties,
            ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, applicationContext);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, defaults());
        Throwable error = getError(request);
        // Default error response
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromValue(errorPropertiesMap);

        if(error instanceof ResponseStatusException) {
            status = ((ResponseStatusException) error).getStatus();
        }

        if (error instanceof ClientError) {
            status = ((ClientError) error).getHttpStatus();
            bodyInserter = BodyInserters.fromValue(((ClientError) error).getError());
        }

        if (error instanceof DbOperationError) {
            log.error(error.getCause().getMessage(), error.getCause());
            status = ((DbOperationError) error).getHttpStatus();
            bodyInserter = BodyInserters.fromValue(((DbOperationError) error).getError());
        }

        if (error instanceof WebExchangeBindException) {
            WebExchangeBindException bindException = (WebExchangeBindException) error;
            FieldError fieldError = bindException.getFieldError();
            if (fieldError != null) {
                String errorMsg = String.format("%s: %s", fieldError.getField(), fieldError.getDefaultMessage());
                ErrorRepresentation errorRepresentation = ErrorRepresentation.builder().error(new Error(ErrorCode.INVALID_REQUEST, errorMsg)).build();
                bodyInserter = BodyInserters.fromValue(errorRepresentation);
                return ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(bodyInserter);
            }
        }

        if (error instanceof ServerWebInputException) {
            ServerWebInputException inputException = (ServerWebInputException) error;
            status = inputException.getStatus();
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyInserter);
    }
}
