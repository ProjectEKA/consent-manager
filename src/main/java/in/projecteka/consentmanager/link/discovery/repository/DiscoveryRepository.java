package in.projecteka.consentmanager.link.discovery.repository;

import in.projecteka.consentmanager.clients.ClientError;
import io.vertx.pgclient.PgPool;
import reactor.core.publisher.Mono;

public class DiscoveryRepository {

    private PgPool dbClient;

    public DiscoveryRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    public Mono<Void> insert(String providerId, String patientId, String transactionId) {
        String sql = String.format("insert into discovery_request (transaction_id, patient_id, hip_id) values ('%s', '%s', '%s')", transactionId, patientId, providerId);

        return Mono.create(monoSink -> dbClient.query(sql, handler -> {
            if (handler.failed())
                monoSink.error(ClientError.dbOperationFailed());
            else
                monoSink.success();
        }));
    }
}
