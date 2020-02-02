package in.projecteka.consentmanager.consent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@AllArgsConstructor
public class ConsentArtefactRepository {
    private static final String INSERT_CONSENT_QUERY = "INSERT INTO consent_artefact" +
            " (consent_request_id, consent_artefact_id, patient_id, consent_artefact, signature) VALUES" +
            " ($1, $2, $3, $4, $5)";
    private static final String FAILED_TO_SAVE_CONSENT_REQUEST = "Failed to save consent artefact";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status =$1 WHERE request_id =$2";
    private static final String UNKNOWN_ERROR_OCCURRED = "Unknown error occurred";
    private static final String SELECT_CONSENT_QUERY = "SELECT cr.status, ca.consent_artefact, ca.signature " +
            "FROM consent_request cr INNER JOIN consent_artefact ca ON cr.request_id = ca.consent_request_id " +
            "WHERE ca.consent_artefact_id = $1";
    private PgPool dbClient;

    public Mono<Void> addConsentArtefactAndUpdateStatus(ConsentArtefact consentArtefact,
                                                        String consentRequestId,
                                                        String patientId,
                                                        String signature) {
        return Mono.create(monoSink -> dbClient.getConnection(connection -> {
                    if (connection.failed()) return;
                    SqlConnection sqlConnection = connection.result();
                    Transaction transaction = sqlConnection.begin();
                    transaction.preparedQuery(
                            INSERT_CONSENT_QUERY,
                            Tuple.of(consentRequestId,
                                    consentArtefact.getConsentId(),
                                    patientId,
                                    JsonObject.mapFrom(consentArtefact),
                                    signature),
                            handler -> updateConsentRequest(
                                    consentRequestId,
                                    monoSink,
                                    sqlConnection,
                                    transaction,
                                    handler));
                })
        );
    }

    private void updateConsentRequest(String consentRequestId,
                                      MonoSink<Void> monoSink,
                                      SqlConnection sqlConnection,
                                      Transaction transaction,
                                      AsyncResult<RowSet<Row>> handler) {
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
    }

    public Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_QUERY, Tuple.of(consentId),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new RuntimeException("Failed to retrieve CA.", handler.cause()));
                    } else {
                        RowSet<Row> results = handler.result();
                        if (results.iterator().hasNext()) {
                            Row row = results.iterator().next();
                            JsonObject artefact = (JsonObject) row.getValue("consent_artefact");
                            ConsentArtefact consentArtefact = artefact.mapTo(ConsentArtefact.class);
                            ConsentArtefactRepresentation representation = ConsentArtefactRepresentation
                                    .builder()
                                    .status(getConsentStatus(row.getString("status")))
                                    .consentDetail(consentArtefact)
                                    .signature(row.getString("signature"))
                                    .build();
                            monoSink.success(representation);
                        } else {
                            monoSink.success(null);
                        }
                    }
                }));
    }

    private ConsentStatus getConsentStatus(String status) {
        return ConsentStatus.valueOf(status);
    }

    @SneakyThrows
    private ConsentArtefact convertToConsentDetail(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonStr.getBytes(), ConsentArtefact.class);
    }
}
