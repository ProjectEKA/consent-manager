package in.projecteka.consentmanager.consent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import io.vertx.core.json.JsonObject;
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
    private static final String SELECT_CONSENT_REQUEST_QUERY = "SELECT request_id, status, details, date_created FROM consent_request where request_id=$1 and status=$2";
    private static final String FAILED_TO_SAVE_CONSENT_REQUEST = "Failed to save consent request";
    private static final String SELECT_CONSENT_DETAILS_FOR_PATIENT = "SELECT request_id, status, details, date_created FROM consent_request where patient_id=$1 LIMIT $2 OFFSET $3";
    private static final String UNKNOWN_ERROR_OCCURRED = "Unknown error occurred";
    private PgPool dbClient;

    public ConsentRequestRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Mono<Void> insert(RequestedDetail requestedDetail, String requestId) {
        final String detailAsString = new ObjectMapper().writeValueAsString(requestedDetail);
        JsonObject detailsJson = new JsonObject(detailAsString);
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_CONSENT_REQUEST_QUERY,
                        Tuple.of(requestId, requestedDetail.getPatient().getId(), ConsentStatus.REQUESTED.name(), detailsJson),
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
        RequestedDetail details = convertToConsentDetail(result.getValue("details").toString());
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
                .build();
    }

    private ConsentStatus getConsentStatus(String status) {
        return ConsentStatus.valueOf(status);
    }

    private Date convertToDate(LocalDateTime timestamp) {
        return Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
    }

    @SneakyThrows
    private RequestedDetail convertToConsentDetail(String details) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(details.getBytes(), RequestedDetail.class);
    }
}
