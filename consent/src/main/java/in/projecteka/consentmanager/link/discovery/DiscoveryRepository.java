package in.projecteka.consentmanager.link.discovery;

import in.projecteka.library.common.DbOperation;
import in.projecteka.library.common.DbOperationError;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class DiscoveryRepository {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryRepository.class);

    private static final String INSERT_TO_DISCOVERY_REQUEST = "INSERT INTO discovery_request " +
            "(transaction_id, request_id, patient_id, hip_id) VALUES ($1, $2, $3, $4)";
    private static final String SELECT_TRANSACTION_ID = "SELECT transaction_id FROM discovery_request WHERE " +
            "request_id=$1";
    private final PgPool dbClient;

    public DiscoveryRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    public Mono<Void> insert(String providerId, String patientId, UUID transactionId, UUID requestId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_TO_DISCOVERY_REQUEST)
                        .execute(Tuple.of(transactionId.toString(), requestId.toString(), patientId, providerId),
                                handler -> {
                                    if (handler.failed()) {
                                        logger.error(handler.cause().getMessage(), handler.cause());
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }

    public Mono<String> getIfPresent(UUID requestId) {
        return DbOperation.select(requestId, dbClient, SELECT_TRANSACTION_ID, row -> row.getString(0));
    }
}
