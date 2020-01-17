package in.projecteka.consentmanager.consent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.consent.model.request.ConsentDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentStatus;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

public class ConsentRequestRepository {
    private PgPool dbClient;

    public ConsentRequestRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Mono<Void> insert(ConsentDetail consentDetail, String requestId) {
        final String s = new ObjectMapper().writeValueAsString(consentDetail);
        return Mono.create(monoSink -> {
                    dbClient.preparedQuery(
                            "INSERT INTO consent_request (request_id, patient_id, status, details) VALUES ($1, $2, $3, $4)",
                            Tuple.of(requestId, consentDetail.getPatient().getId(), ConsentStatus.REQUESTED.name(), s),
                            handler -> {
                                if (handler.failed())
                                    monoSink.error(new Exception("Failed to save consent request"));
                                else
                                    monoSink.success();
                            });
                }
        );
    }
}
