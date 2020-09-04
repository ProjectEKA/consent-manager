package in.projecteka.consentmanager;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Map;
import java.util.UUID;

import static in.projecteka.library.common.Constants.CORRELATION_ID;

@Component
@Slf4j
public class CorrelationIDFilter implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIDFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Map<String, String> headers = exchange.getRequest().getHeaders().toSingleValueMap();
        var path = exchange.getRequest().getPath().toString();
        var requestMethod = exchange.getRequest().getMethod();

        return chain.filter(exchange)
                .subscriberContext(context -> {
                    var correlationId = "";
                    if (headers.containsKey(CORRELATION_ID)) {
                        correlationId = headers.get(CORRELATION_ID);
                    } else {
                        correlationId = generateRandomCorrelationId();
                    }
                    MDC.put(CORRELATION_ID, correlationId);
                    if (!path.endsWith("/heartbeat")){
                        logger.info("Received a request for path: {}, method: {}", path, requestMethod);
                    }
                    Context contextTmp = context.put(CORRELATION_ID, correlationId);
                    exchange.getAttributes().put(CORRELATION_ID, correlationId);
                    return contextTmp;
                });
    }

    private String generateRandomCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
