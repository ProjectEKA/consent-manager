package in.org.projecteka.hdaf.link.link;

import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

public class ClientErrorExceptionHandler extends AbstractErrorWebExceptionHandler {
    public ClientErrorExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties, ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, applicationContext);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(
            ErrorAttributes errorAttributes) {
        return RouterFunctions.route(
                RequestPredicates.all(), this::renderErrorResponse
        );
    }

    private Mono<ServerResponse> renderErrorResponse(
            ServerRequest request) {
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, false);
        Throwable error = getError(request);
        // Default error response
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        BodyInserter<Object, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromValue(errorPropertiesMap);

        if (error instanceof ClientError) {
            status = ((ClientError) error).getHttpStatus();
            bodyInserter = BodyInserters.fromValue(((ClientError) error).getError());
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyInserter);
    }
}
