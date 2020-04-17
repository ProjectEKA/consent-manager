package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.DbOperationError;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;

public class DiscoveryRepository {

    private static final String INSERT_TO_DISCOVERY_REQUEST = "INSERT INTO discovery_request (transaction_id, " +
            "patient_id, hip_id) VALUES ($1, $2, $3)";
    private final PgPool dbClient;

    public DiscoveryRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    public Mono<Void> insert(String providerId, String patientId, String transactionId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_TO_DISCOVERY_REQUEST)
                        .execute(Tuple.of(transactionId, patientId, providerId),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }
}
