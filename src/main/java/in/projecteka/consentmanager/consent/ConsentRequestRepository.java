package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static in.projecteka.consentmanager.common.Serializer.from;
import static in.projecteka.consentmanager.common.Serializer.to;

public class ConsentRequestRepository {
    private static final String SELECT_CONSENT_REQUEST_BY_ID_AND_STATUS;
    private static final String SELECT_CONSENT_REQUEST_BY_ID;
    private static final String SELECT_CONSENT_DETAILS_FOR_PATIENT;
    private static final String INSERT_CONSENT_REQUEST_QUERY = "INSERT INTO consent_request " +
            "(consent_request_id, patient_id, status, details, request_id) VALUES ($1, $2, $3, $4, $5)";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status=$1, " +
            "date_modified=$2 WHERE consent_request_id=$3";
    private static final String FAILED_TO_SAVE_CONSENT_REQUEST = "Failed to save consent request";
    private static final String UNKNOWN_ERROR_OCCURRED = "Unknown error occurred";
    private final PgPool dbClient;

    static {
        String s = "SELECT consent_request_id, status, details, date_created, date_modified FROM consent_request " +
                "where ";
        SELECT_CONSENT_DETAILS_FOR_PATIENT = s + "patient_id=$1 LIMIT $2 OFFSET $3";
        SELECT_CONSENT_REQUEST_BY_ID = s + "consent_request_id=$1";
        SELECT_CONSENT_REQUEST_BY_ID_AND_STATUS = s + "consent_request_id=$1 and status=$2 and patient_id=$3";
    }

    public ConsentRequestRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    public Mono<Void> insert(RequestedDetail requestedDetail, String consentRequestId, String requestId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_CONSENT_REQUEST_QUERY)
                        .execute(Tuple.of(consentRequestId,
                                requestedDetail.getPatient().getId(),
                                ConsentStatus.REQUESTED.name(),
                                new JsonObject(from(requestedDetail)),
                                requestId),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new Exception(FAILED_TO_SAVE_CONSENT_REQUEST));
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }

    public Mono<List<ConsentRequestDetail>> requestsForPatient(String patientId, int limit, int offset) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_DETAILS_FOR_PATIENT)
                .execute(Tuple.of(patientId, limit, offset),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new RuntimeException(UNKNOWN_ERROR_OCCURRED));
                                return;
                            }
                            List<ConsentRequestDetail> requestList = new ArrayList<>();
                            RowSet<Row> results = handler.result();
                            for (Row result : results) {
                                ConsentRequestDetail aDetail = mapToConsentRequestDetail(result);
                                requestList.add(aDetail);
                            }
                            monoSink.success(requestList);
                        }));
    }

    public Mono<ConsentRequestDetail> requestOf(String requestId, String status, String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_REQUEST_BY_ID_AND_STATUS)
                .execute(Tuple.of(requestId, status, patientId),
                        consentRequestHandler(monoSink)));
    }

    public Mono<ConsentRequestDetail> requestOf(String requestId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_REQUEST_BY_ID)
                .execute(Tuple.of(requestId),
                        consentRequestHandler(monoSink)));
    }

    private Handler<AsyncResult<RowSet<Row>>> consentRequestHandler(MonoSink<ConsentRequestDetail> monoSink) {
        return handler -> {
            if (handler.failed()) {
                monoSink.error(new RuntimeException(UNKNOWN_ERROR_OCCURRED));
                return;
            }
            RowSet<Row> results = handler.result();
            ConsentRequestDetail consentRequestDetail = null;
            for (Row result : results) {
                consentRequestDetail = mapToConsentRequestDetail(result);
            }
            monoSink.success(consentRequestDetail);
        };
    }

    private ConsentRequestDetail mapToConsentRequestDetail(Row result) {
        RequestedDetail details = to(result.getValue("details").toString(), RequestedDetail.class);
        return ConsentRequestDetail
                .builder()
                .requestId(result.getString("request_id"))
                .status(getConsentStatus(result.getString("status")))
                .createdAt(convertToDate(result.getLocalDateTime("date_created")))
                .hip(details.getHip())
                .hiu(details.getHiu())
                .hiTypes(details.getHiTypes())
                .patient(details.getPatient())
                .permission(details.getPermission())
                .purpose(details.getPurpose())
                .requester(details.getRequester())
                .consentNotificationUrl(details.getConsentNotificationUrl())
                .lastUpdated(convertToDate(result.getLocalDateTime("date_modified")))
                .build();
    }

    public Mono<Void> updateStatus(String id, ConsentStatus status) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_CONSENT_REQUEST_STATUS_QUERY)
                .execute(Tuple.of(status.toString(), LocalDateTime.now(), id),
                        updateHandler -> {
                            if (updateHandler.failed()) {
                                monoSink.error(new Exception("Failed to update status"));
                            }
                            monoSink.success();
                        }));
    }

    private ConsentStatus getConsentStatus(String status) {
        return ConsentStatus.valueOf(status);
    }

    private Date convertToDate(LocalDateTime timestamp) {
        if (timestamp != null) {
            return Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }
}
