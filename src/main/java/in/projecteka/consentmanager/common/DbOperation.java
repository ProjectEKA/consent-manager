package in.projecteka.consentmanager.common;

import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;

public class DbOperation {
    public static Mono<Boolean> getBooleanMono(String requestId, PgPool dbClient, String query) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(query)
                        .execute(Tuple.of(requestId),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    var iterator = handler.result().iterator();
                                    if (!iterator.hasNext()) {
                                        monoSink.error(unknownErrorOccurred());
                                        return;
                                    }
                                    monoSink.success(iterator.next().getBoolean(0));
                                }));
    }
}
