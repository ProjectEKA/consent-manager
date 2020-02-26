package in.projecteka.consentmanager.consent.repository;

import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

@AllArgsConstructor
public class ConsentArtefactRepository {
    private static final String INSERT_CONSENT_ARTEFACT_QUERY = "INSERT INTO consent_artefact" +
            " (consent_request_id, consent_artefact_id, patient_id, consent_artefact, signature, status) VALUES" +
            " ($1, $2, $3, $4, $5, $6)";
    private static final String INSERT_HIP_CONSENT_ARTEFACT_QUERY = "INSERT INTO hip_consent_artefact" +
            " (consent_request_id, consent_artefact_id, patient_id, consent_artefact, signature, status) VALUES" +
            " ($1, $2, $3, $4, $5, $6)";
    private static final String FAILED_TO_SAVE_CONSENT_ARTEFACT = "Failed to save consent artefact";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status=$1, " +
            "date_modified=$2 WHERE request_id=$3";
    private static final String UNKNOWN_ERROR_OCCURRED = "Unknown error occurred";
    private static final String SELECT_CONSENT_QUERY = "SELECT status, consent_artefact, signature " +
            "FROM consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_HIP_CONSENT_QUERY = "SELECT status, consent_artefact, signature " +
            "FROM hip_consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_CONSENT_IDS_FROM_CONSENT_ARTIFACT = "SELECT consent_artefact_id " +
            "FROM consent_artefact WHERE consent_request_id=$1";
    private PgPool dbClient;

    public Mono<Void> addConsentArtefactAndUpdateStatus(ConsentArtefact consentArtefact,
                                                        String consentRequestId,
                                                        String patientId,
                                                        String signature,
                                                        HIPConsentArtefactRepresentation hipConsentArtefact) {
        return Mono.create(monoSink -> dbClient.getConnection(connection -> {
                    if (connection.failed()) return;
                    SqlConnection sqlConnection = connection.result();
                    Transaction transaction = sqlConnection.begin();
                    transaction.preparedQuery(
                            INSERT_CONSENT_ARTEFACT_QUERY,
                            Tuple.of(consentRequestId,
                                    consentArtefact.getConsentId(),
                                    patientId,
                                    JsonObject.mapFrom(consentArtefact),
                                    signature,
                                    ConsentStatus.GRANTED.toString()),
                            insertConsentArtefactHandler -> insertHipConsentArtefact(hipConsentArtefact,
                                    consentRequestId,
                                    patientId,
                                    signature,
                                    monoSink,
                                    sqlConnection,
                                    transaction,
                                    insertConsentArtefactHandler));
                })
        );
    }

    private void insertHipConsentArtefact(HIPConsentArtefactRepresentation consentArtefact,
                                          String consentRequestId,
                                          String patientId,
                                          String signature,
                                          MonoSink<Void> monoSink,
                                          SqlConnection sqlConnection,
                                          Transaction transaction,
                                          AsyncResult<RowSet<Row>> insertConsentArtefactHandler) {
        if (insertConsentArtefactHandler.failed()) {
            sqlConnection.close();
            monoSink.error(new Exception(FAILED_TO_SAVE_CONSENT_ARTEFACT));
        } else {
            transaction.preparedQuery(
                    INSERT_HIP_CONSENT_ARTEFACT_QUERY,
                    Tuple.of(consentRequestId,
                            consentArtefact.getConsentDetail().getConsentId(),
                            patientId,
                            JsonObject.mapFrom(consentArtefact.getConsentDetail()),
                            signature,
                            ConsentStatus.GRANTED.toString()),
                    insertHipConsentArtefactHandler -> updateConsentRequest(
                            consentRequestId,
                            monoSink,
                            sqlConnection,
                            transaction,
                            insertHipConsentArtefactHandler)
            );
        }
    }

    private void updateConsentRequest(String consentRequestId,
                                      MonoSink<Void> monoSink,
                                      SqlConnection sqlConnection,
                                      Transaction transaction,
                                      AsyncResult<RowSet<Row>> insertHipConsentArtefactHandler) {
        if (insertHipConsentArtefactHandler.failed()) {
            sqlConnection.close();
            monoSink.error(new Exception(FAILED_TO_SAVE_CONSENT_ARTEFACT));
        } else {
            transaction.preparedQuery(
                    UPDATE_CONSENT_REQUEST_STATUS_QUERY,
                    Tuple.of(ConsentStatus.GRANTED.toString(), LocalDateTime.now(), consentRequestId),
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
                                    .status(ConsentStatus.valueOf(row.getString("status")))
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

    public Mono<HIPConsentArtefactRepresentation> getHipConsentArtefact(String consentId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_HIP_CONSENT_QUERY, Tuple.of(consentId),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new RuntimeException("Failed to retrieve CA.", handler.cause()));
                    } else {
                        RowSet<Row> results = handler.result();
                        if (results.iterator().hasNext()) {
                            Row row = results.iterator().next();
                            JsonObject artefact = (JsonObject) row.getValue("consent_artefact");
                            HIPConsentArtefact consentArtefact = artefact.mapTo(HIPConsentArtefact.class);
                            HIPConsentArtefactRepresentation representation = HIPConsentArtefactRepresentation
                                    .builder()
                                    .status(ConsentStatus.valueOf(row.getString("status")))
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

    public Flux<String> getConsentArtefacts(String consentRequestId) {
        return Flux.create(fluxSink -> dbClient.preparedQuery(SELECT_CONSENT_IDS_FROM_CONSENT_ARTIFACT,
                Tuple.of(consentRequestId),
                handler -> {
                    if (handler.failed()) {
                        fluxSink.error(new Exception("Failed to get consent id from consent request Id"));
                    } else {
                        StreamSupport.stream(handler.result().spliterator(), false)
                                .map(row -> row.getString("consent_artefact_id"))
                                .forEach(fluxSink::next);
                        fluxSink.complete();
                    }
                }));
    }
}
