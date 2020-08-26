package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestStatus;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
import in.projecteka.library.common.DbOperation;
import in.projecteka.library.common.DbOperationError;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static in.projecteka.library.clients.model.ClientError.unknownErrorOccurred;
import static in.projecteka.library.common.Serializer.from;

public class DataFlowRequestRepository {

    private static final Logger logger = LoggerFactory.getLogger(DataFlowRequestRepository.class);

    private static final String INSERT_TO_DATA_FLOW_REQUEST = "INSERT INTO data_flow_request (transaction_id, " +
            "consent_artefact_id, data_flow_request) VALUES ($1, $2, $3)";
    private static final String INSERT_TO_HEALTH_INFO_NOTIFICATION = "INSERT INTO health_info_notification " +
            "(transaction_id, notification_request, request_id) VALUES ($1, $2, $3)";
    private static final String SELECT_TRANSACTION_ID = "SELECT transaction_id FROM health_info_notification WHERE " +
            "request_id=$1";
    private static final String UPDATE_DATA_FLOW_REQUEST_STATUS = "UPDATE data_flow_request SET status = $1 WHERE " +
            "transaction_id = $2";

    private final PgPool dbClient;

    public DataFlowRequestRepository(PgPool pgPool) {
        this.dbClient = pgPool;
    }

    public Mono<Void> addDataFlowRequest(String transactionId, DataFlowRequest dataFlowRequest) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_TO_DATA_FLOW_REQUEST)
                .execute(Tuple.of(transactionId,
                        dataFlowRequest.getConsent().getId(),
                        new JsonObject(from(dataFlowRequest))),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> saveHealthNotificationRequest(HealthInfoNotificationRequest notificationRequest) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_TO_HEALTH_INFO_NOTIFICATION)
                        .execute(Tuple.of(notificationRequest.getNotification().getTransactionId(),
                                new JsonObject(from(notificationRequest)),
                                notificationRequest.getRequestId().toString()),
                                handler -> {
                                    if (handler.failed()) {
                                        logger.error(handler.cause().getMessage(), handler.cause());
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }

    public Mono<String> getIfPresent(UUID requestId) {
        return DbOperation.select(requestId, dbClient, SELECT_TRANSACTION_ID, row -> row.getString(0));
    }

    public Mono<Void> updateDataFlowRequestStatus(String transactionId, DataFlowRequestStatus status) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(UPDATE_DATA_FLOW_REQUEST_STATUS)
                        .execute(Tuple.of(status.name(), transactionId),
                                handler -> {
                                    if (handler.failed()) {
                                        logger.error(handler.cause().getMessage(), handler.cause());
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }
}
