package in.projecteka.consentmanager.consent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ConsentArtefactRepository {
    private static final String INSERT_CONSENT_QUERY = "INSERT INTO consent_artefact" +
            " (consent_request_id, consent_artefact_id, patient_id, consent_artefact, signature) VALUES" +
            " ($1, $2, $3, $4, $5)";
    private static final String FAILED_TO_SAVE_CONSENT_REQUEST = "Failed to save consent artefact";
    private PgPool dbClient;

    @SneakyThrows
    public Mono<Void> insert(ConsentArtefact consentArtefact,
                             String consentArtefactId,
                             String consentRequestId,
                             String patientId,
                             String signature) {
        final String consentArtefactJson = new ObjectMapper().writeValueAsString(consentArtefact);
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_CONSENT_QUERY,
                        Tuple.of(consentRequestId, consentArtefactId, patientId, consentArtefactJson, signature),
                        handler -> {
                            if (handler.failed())
                                monoSink.error(new Exception(FAILED_TO_SAVE_CONSENT_REQUEST));
                            else
                                monoSink.success();
                        })
        );
    }
}
