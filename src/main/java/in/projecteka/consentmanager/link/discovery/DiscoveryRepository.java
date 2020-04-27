package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.common.DbOperation;
import in.projecteka.consentmanager.common.DbOperationError;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;

public class DiscoveryRepository {

    private static final String INSERT_TO_DISCOVERY_REQUEST = "INSERT INTO discovery_request " +
            "(transaction_id, request_id, patient_id, hip_id) VALUES ($1, $2, $3, $4)";
    private static final String CHECK_REQUEST_ID_EXISTS = "SELECT exists(SELECT * FROM discovery_request WHERE " +
            "request_id=$1)";
    private final PgPool dbClient;

    public DiscoveryRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    public Mono<Void> insert(String providerId, String patientId, String transactionId, String requestId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_TO_DISCOVERY_REQUEST)
                        .execute(Tuple.of(transactionId, requestId, patientId, providerId),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }

    public Mono<Boolean> isRequestPresent(String requestId) {
        return DbOperation.getBooleanMono(requestId, dbClient, CHECK_REQUEST_ID_EXISTS);
    }
}
