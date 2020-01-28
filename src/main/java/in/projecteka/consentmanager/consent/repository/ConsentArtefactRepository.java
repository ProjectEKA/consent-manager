package in.projecteka.consentmanager.consent.repository;

import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.response.ConsentStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ConsentArtefactRepository {
    private static final String INSERT_CONSENT_QUERY = "INSERT INTO consent_artefact" +
            " (consent_request_id, consent_artefact_id, patient_id, consent_artefact, signature) VALUES" +
            " ($1, $2, $3, $4, $5)";
    private static final String FAILED_TO_SAVE_CONSENT_REQUEST = "Failed to save consent artefact";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status =$1 WHERE request_id =$2";
    private static final String UNKNOWN_ERROR_OCCURRED = "Unknown error occurred";
    private PgPool dbClient;

    public Mono<Void> addConsentArtefactAndUpdateStatus(ConsentArtefact consentArtefact,
                                                        String consentRequestId,
                                                        String patientId,
                                                        byte[] signature) {
        return Mono.create(monoSink -> dbClient.getConnection(connection -> {
                    if (connection.succeeded()) {
                        SqlConnection sqlConnection = connection.result();
                        Transaction transaction = sqlConnection.begin();

                        transaction.preparedQuery(
                                INSERT_CONSENT_QUERY,
                                Tuple.of(consentRequestId, consentArtefact.getId(), patientId, JsonObject.mapFrom(consentArtefact), Buffer.buffer(signature)),
                                handler -> {
                                    if (handler.failed()) {
                                        sqlConnection.close();
                                        monoSink.error(new Exception(FAILED_TO_SAVE_CONSENT_REQUEST));
                                    } else {
                                        transaction.preparedQuery(
                                                UPDATE_CONSENT_REQUEST_STATUS_QUERY,
                                                Tuple.of(ConsentStatus.GRANTED.toString(), consentRequestId),
                                                updateConsentRequestHandler -> {
                                                    if (updateConsentRequestHandler.failed()) {
                                                        sqlConnection.close();
                                                        monoSink.error(new Exception(UNKNOWN_ERROR_OCCURRED));
                                                    } else {
                                                        transaction.commit();
                                                        monoSink.success();
                                                    }
                                                });
                                    }
                                });
                    }
                })
        );
    }

}
