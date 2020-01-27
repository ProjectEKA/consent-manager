package in.projecteka.consentmanager.consent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.consent.model.request.ConsentDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentStatus;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsentRequestRepository {
    private static final String INSERT_CONSENT_REQUEST_QUERY = "INSERT INTO consent_request (request_id, patient_id, status, details) VALUES ($1, $2, $3, $4)";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status =$1 WHERE request_id =$2";
    private static final String SELECT_CONSENT_REQUEST_QUERY = "SELECT request_id, status, details, timestamp FROM consent_request where request_id=$1 and status=$2";
    private static final String FAILED_TO_SAVE_CONSENT_REQUEST = "Failed to save consent request";
    public static final String SELECT_CONSENT_DETAILS_FOR_PATIENT = "SELECT request_id, status, details, timestamp FROM consent_request where patient_id=$1 LIMIT $2 OFFSET $3";
    public static final String UNKNOWN_ERROR_OCCURRED = "Unknown error occurred";
    private PgPool dbClient;

    public ConsentRequestRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Mono<Void> insert(ConsentDetail consentDetail, String requestId) {
        final String detailAsString = new ObjectMapper().writeValueAsString(consentDetail);
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_CONSENT_REQUEST_QUERY,
                        Tuple.of(requestId, consentDetail.getPatient().getId(), ConsentStatus.REQUESTED.name(), detailAsString),
                        handler -> {
                            if (handler.failed())
                                monoSink.error(new Exception(FAILED_TO_SAVE_CONSENT_REQUEST));
                            else
                                monoSink.success();
                        })
        );
    }

    public Mono<List<ConsentRequestDetail>> requestsForPatient(String patientId, int limit, int offset) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_DETAILS_FOR_PATIENT, Tuple.of(patientId, limit, offset),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new RuntimeException(UNKNOWN_ERROR_OCCURRED));
                    } else {
                        List<ConsentRequestDetail> requestList = new ArrayList<>();
                        RowSet<Row> results = handler.result();
                        for (Row result : results) {
                            ConsentRequestDetail aDetail = mapToConsentRequestDetail(result);
                            requestList.add(aDetail);
                        }
                        monoSink.success(requestList);
                    }
                }));
    }

    public Mono<ConsentRequestDetail> requestOf(String requestId, String status) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CONSENT_REQUEST_QUERY, Tuple.of(requestId, status),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new RuntimeException(handler.cause().getMessage()));
                    } else {
                        RowSet<Row> results = handler.result();
                        ConsentRequestDetail consentRequestDetail = null;
                        for (Row result : results) {
                            consentRequestDetail = mapToConsentRequestDetail(result);
                        }
                        monoSink.success(consentRequestDetail);
                    }
                }));
    }

    private ConsentRequestDetail mapToConsentRequestDetail(Row result) {
        ConsentDetail details = convertToConsentDetail(result.getString("details"));
        return ConsentRequestDetail
                .builder()
                .requestId(result.getString("request_id"))
                .status(getConsentStatus(result.getString("status")))
                .createdAt(convertToDate(result.getLocalDateTime("timestamp")))
                .hip(details.getHip())
                .hiu(details.getHiu())
                .hiTypes(details.getHiTypes())
                .patient(details.getPatient())
                .permission(details.getPermission())
                .purpose(details.getPurpose())
                .requester(details.getRequester())
                .build();
    }

    private ConsentStatus getConsentStatus(String status) {
        return ConsentStatus.valueOf(status);
    }

    private Date convertToDate(LocalDateTime timestamp) {
        return Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
    }

    @SneakyThrows
    private ConsentDetail convertToConsentDetail(String details) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(details.getBytes(), ConsentDetail.class);
    }

    @SneakyThrows
    public Mono<Void> updateStatus(String requestId, String status) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        UPDATE_CONSENT_REQUEST_STATUS_QUERY,
                        Tuple.of(status, requestId),
                        handler -> {
                            if (handler.failed()){
                                monoSink.error(new Exception(UNKNOWN_ERROR_OCCURRED));
                            }
                            else
                                monoSink.success();
                        })
        );
    }
}
