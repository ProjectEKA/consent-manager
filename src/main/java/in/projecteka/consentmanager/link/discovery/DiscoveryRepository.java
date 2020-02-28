package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.ClientError;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;

public class DiscoveryRepository {

    private static final String INSERT_TO_DISCOVERY_REQUEST = "INSERT INTO discovery_request (transaction_id, " +
            "patient_id, hip_id) VALUES ($1, $2, $3)";
    private PgPool dbClient;

    public DiscoveryRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    public Mono<Void> insert(String providerId, String patientId, String transactionId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_TO_DISCOVERY_REQUEST,
                        Tuple.of(transactionId, patientId, providerId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.dbOperationFailed());
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
