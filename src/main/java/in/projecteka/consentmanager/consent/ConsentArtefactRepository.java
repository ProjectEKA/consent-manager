package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.Query;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

@AllArgsConstructor
public class ConsentArtefactRepository {
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status=$1, " +
            "date_modified=$2 WHERE consent_request_id=$3";
    private static final String SELECT_CONSENT_QUERY = "SELECT status, consent_artefact, signature " +
            "FROM consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_CONSENT_WITH_REQUEST_QUERY = "SELECT status, consent_artefact, " +
            "consent_request_id, date_modified FROM consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_HIP_CONSENT_QUERY = "SELECT status, consent_artefact, signature " +
            "FROM hip_consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_CONSENT_IDS_FROM_CONSENT_ARTEFACT = "SELECT consent_artefact_id " +
            "FROM consent_artefact WHERE consent_request_id=$1";
    private static final String UPDATE_CONSENT_ARTEFACT_STATUS_QUERY = "UPDATE consent_artefact SET status=$1, " +
            "date_modified=$2 WHERE consent_artefact_id=$3";
    private static final String FAILED_TO_RETRIEVE_CA = "Failed to retrieve Consent Artifact.";
    private static final String FAILED_TO_SAVE_CONSENT_ARTEFACT = "Failed to save consent artefact";

    private final PgPool dbClient;

    public Mono<Void> process(List<Query> queries) {
        return doInTransaction(queries);
    }

    private Mono<Void> doInTransaction(List<Query> queries) {
        return Mono.create(monoSink -> dbClient.begin(connectionAttempt -> {
            if (connectionAttempt.succeeded()) {
                TransactionContext context = new TransactionContext(connectionAttempt.result(), monoSink);
                context.executeInTransaction(queries.iterator(), FAILED_TO_SAVE_CONSENT_ARTEFACT);
            } else {
                monoSink.error(new RuntimeException("Can not get connectionAttempt to storage."));
            }
        }));
    }

    public Mono<ConsentArtefactRepresentation> getConsentArtefact(String consentId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_QUERY)
                .execute(Tuple.of(consentId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new RuntimeException(FAILED_TO_RETRIEVE_CA, handler.cause()));
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
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_HIP_CONSENT_QUERY)
                .execute(Tuple.of(consentId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new RuntimeException(FAILED_TO_RETRIEVE_CA, handler.cause()));
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
        return Flux.create(fluxSink -> dbClient.preparedQuery(SELECT_CONSENT_IDS_FROM_CONSENT_ARTEFACT)
                .execute(Tuple.of(consentRequestId),
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

    public Mono<Void> updateStatus(String consentId, String consentRequestId, ConsentStatus status) {
        return Mono.create(monoSink -> dbClient.begin(connectionAttempt -> {
            List<Query> queries = getUpdateQueries(consentId, consentRequestId, status);
            if (connectionAttempt.succeeded()) {
                TransactionContext context = new TransactionContext(connectionAttempt.result(), monoSink);
                context.executeInTransaction(queries.iterator(), "Failed to update status");
            } else {
                monoSink.error(new RuntimeException("Can not get connectionAttempt to storage."));
            }
        }));
    }

    private List<Query> getUpdateQueries(String consentId, String consentRequestId, ConsentStatus status) {
        Query consentRequestUpdate = new Query(UPDATE_CONSENT_REQUEST_STATUS_QUERY,
                Tuple.of(status.toString(),
                        LocalDateTime.now(),
                        consentRequestId));
        Query consentArtefactUpdate = new Query(UPDATE_CONSENT_ARTEFACT_STATUS_QUERY,
                Tuple.of(status.toString(),
                        LocalDateTime.now(),
                        consentId));
        return List.of(consentRequestUpdate, consentArtefactUpdate);
    }

    public Mono<ConsentRepresentation> getConsentWithRequest(String consentId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_WITH_REQUEST_QUERY)
                .execute(Tuple.of(consentId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new RuntimeException(FAILED_TO_RETRIEVE_CA, handler.cause()));
                            } else {
                                RowSet<Row> results = handler.result();
                                if (results.iterator().hasNext()) {
                                    Row row = results.iterator().next();
                                    JsonObject artefact = (JsonObject) row.getValue("consent_artefact");
                                    ConsentArtefact consentArtefact = artefact.mapTo(ConsentArtefact.class);
                                    ConsentRepresentation representation = ConsentRepresentation
                                            .builder()
                                            .status(ConsentStatus.valueOf(row.getString("status")))
                                            .consentDetail(consentArtefact)
                                            .consentRequestId(row.getString("consent_request_id"))
                                            .dateModified(convertToDate(row.getLocalDateTime("date_modified")))
                                            .build();
                                    monoSink.success(representation);
                                } else {
                                    monoSink.success(null);
                                }
                            }
                        }));
    }

    private Date convertToDate(LocalDateTime timestamp) {
        if (timestamp != null) {
            return Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }
}
