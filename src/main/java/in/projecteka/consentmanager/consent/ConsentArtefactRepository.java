package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentExpiry;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefact;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.Query;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

import static in.projecteka.consentmanager.common.Serializer.to;

@AllArgsConstructor
public class ConsentArtefactRepository {
    public static final String CONSENT_ARTEFACT = "consent_artefact";
    public static final String STATUS = "status";
    public static final String CONSENT_REQUEST_ID = "consent_request_id";
    public static final String DATE_MODIFIED = "date_modified";
    public static final String CONSENT_ARTEFACT_ID = "consent_artefact_id";
    public static final String SIGNATURE = "signature";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status=$1, " +
            "date_modified=$2 WHERE request_id=$3";
    private static final String SELECT_CONSENT_QUERY = "SELECT status, consent_artefact, signature " +
            "FROM consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_CONSENT_WITH_REQUEST_QUERY = "SELECT status, consent_artefact, " +
            "consent_request_id, date_modified FROM consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_HIP_CONSENT_QUERY = "SELECT status, consent_artefact, signature " +
            "FROM hip_consent_artefact WHERE consent_artefact_id = $1";
    private static final String SELECT_CONSENT_IDS_FROM_CONSENT_ARTEFACT = "SELECT consent_artefact_id " +
            "FROM consent_artefact WHERE consent_request_id=$1";
    private static final String SELECT_CONSENTS_TO_VALIDATE_EXPIRY = "SELECT consent_artefact_id, " +
            "consent_artefact -> 'permission' ->> 'dataEraseAt' as consent_expiry_date, patient_id " +
            "FROM consent_artefact WHERE status=$1";
    private static final String UPDATE_CONSENT_ARTEFACT_STATUS_QUERY = "UPDATE consent_artefact SET status=$1, " +
            "date_modified=$2 WHERE consent_artefact_id=$3";
    private static final String SELECT_ALL_CONSENT_ARTEFACTS = "SELECT status, consent_artefact, signature FROM consent_artefact";
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
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            if (!results.iterator().hasNext()) {
                                monoSink.success();
                                return;
                            }
                            Row row = results.iterator().next();
                            monoSink.success(getConsentArtefactRepresentation(row));
                        }));
    }

    public Mono<HIPConsentArtefactRepresentation> getHipConsentArtefact(String consentId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_HIP_CONSENT_QUERY)
                .execute(Tuple.of(consentId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new RuntimeException(FAILED_TO_RETRIEVE_CA, handler.cause()));
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            if (!results.iterator().hasNext()) {
                                monoSink.success(null);
                                return;
                            }
                            Row row = results.iterator().next();
                            var consentArtefact = to(row.getValue(CONSENT_ARTEFACT).toString(),
                                    HIPConsentArtefact.class);
                            var representation = HIPConsentArtefactRepresentation
                                    .builder()
                                    .status(ConsentStatus.valueOf(row.getString(STATUS)))
                                    .consentDetail(consentArtefact)
                                    .signature(row.getString(SIGNATURE))
                                    .build();
                            monoSink.success(representation);
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
                                        .map(row -> row.getString(CONSENT_ARTEFACT_ID))
                                        .forEach(fluxSink::next);
                                fluxSink.complete();
                            }
                        }));
    }

    public Flux<ConsentArtefactRepresentation> getAllConsentArtefacts() {
        return Flux.create(fluxSink -> dbClient.preparedQuery(SELECT_ALL_CONSENT_ARTEFACTS)
                .execute(handler -> {
                    if (handler.failed()) {
                        fluxSink.error(new DbOperationError());
                        return;
                    }
                    StreamSupport.stream(handler.result().spliterator(), false)
                            .map(row -> getConsentArtefactRepresentation(row))
                            .forEach(fluxSink::next);
                    fluxSink.complete();
                }));
    }

    private ConsentArtefactRepresentation getConsentArtefactRepresentation(Row row) {
        ConsentArtefact consentArtefact = to(row.getValue(CONSENT_ARTEFACT).toString(),
                ConsentArtefact.class);
        return ConsentArtefactRepresentation
                .builder()
                .status(ConsentStatus.valueOf(row.getString(STATUS)))
                .consentDetail(consentArtefact)
                .signature(row.getString(SIGNATURE))
                .build();
    }

    @SneakyThrows
    public Flux<ConsentExpiry> getConsentArtefacts(ConsentStatus consentStatus) {
        return Flux.create(fluxSink -> dbClient.preparedQuery(SELECT_CONSENTS_TO_VALIDATE_EXPIRY)
                .execute(Tuple.of(consentStatus.toString()),
                        handler -> {
                            if (handler.failed()) {
                                fluxSink.error(new Exception("Failed to get GRANTED consents"));
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            if (results.iterator().hasNext()) {
                                results.forEach(row -> {
                                    fluxSink.next(ConsentExpiry.builder()
                                            .consentId(row.getString("consent_artefact_id"))
                                            .patientId(row.getString("patient_id"))
                                            .consentExpiryDate(new Date(Long.parseLong(row.getString("consent_expiry_date"))))
                                            .build());
                                });
                            }
                            fluxSink.complete();
                        }));
    }

    public Mono<Void> updateStatus(String consentId, String consentRequestId, ConsentStatus status) {
        return Mono.create(monoSink -> dbClient.begin(connectionAttempt -> {
            var queries = getUpdateQueries(consentId, consentRequestId, status);
            if (connectionAttempt.succeeded()) {
                TransactionContext context = new TransactionContext(connectionAttempt.result(), monoSink);
                context.executeInTransaction(queries.iterator(), "Failed to update status");
            } else {
                monoSink.error(new RuntimeException("Can not get connectionAttempt to storage."));
            }
        }));
    }

    public Mono<Void> updateConsentArtefactStatus(String consentId, ConsentStatus status) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_CONSENT_ARTEFACT_STATUS_QUERY)
                .execute(Tuple.of(status.toString(),
                        LocalDateTime.now(),
                        consentId),
                        updateHandler -> {
                            if (updateHandler.failed()) {
                                monoSink.error(new Exception("Failed to update consent artefact status"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<ConsentRepresentation> getConsentWithRequest(String consentId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_WITH_REQUEST_QUERY)
                .execute(Tuple.of(consentId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new RuntimeException(FAILED_TO_RETRIEVE_CA, handler.cause()));
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            if (!results.iterator().hasNext()) {
                                monoSink.success();
                                return;
                            }
                            Row row = results.iterator().next();
                            var consentArtefact = to(row.getValue(CONSENT_ARTEFACT).toString(),
                                    ConsentArtefact.class);
                            var representation = ConsentRepresentation
                                    .builder()
                                    .status(ConsentStatus.valueOf(row.getString(STATUS)))
                                    .consentDetail(consentArtefact)
                                    .consentRequestId(row.getString(CONSENT_REQUEST_ID))
                                    .dateModified(convertToDate(row.getLocalDateTime(DATE_MODIFIED)))
                                    .build();
                            monoSink.success(representation);
                        }));
    }

    private Date convertToDate(LocalDateTime timestamp) {
        if (timestamp != null) {
            return Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
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
}
