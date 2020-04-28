package in.projecteka.consentmanager.common;

import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;

public class DbOperation {
    public static Mono<String> select(String requestId, PgPool dbClient, String query) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(query)
                        .execute(Tuple.of(requestId),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    RowSet<Row> results = handler.result();
                                    String value = null;
                                    for (Row result : results) {
                                        value = result.getValue(0).toString();
                                    }
                                    monoSink.success(value);
                                }));
    }
}
