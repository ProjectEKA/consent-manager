package in.projecteka.consentmanager.common;

import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

public class DbOperation {
    public static <T> Mono<T> select(UUID requestId,
                                     PgPool dbClient,
                                     String query,
                                     Function<Row, T> mapper) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(query)
                        .execute(Tuple.of(requestId.toString()),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    var results = handler.result().iterator();
                                    if (!results.hasNext()) {
                                        monoSink.success();
                                        return;
                                    }
                                    monoSink.success(mapper.apply(results.next()));
                                }));
    }
}
