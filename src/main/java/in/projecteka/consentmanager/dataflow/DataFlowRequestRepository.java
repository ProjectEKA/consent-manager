package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import reactor.core.publisher.Mono;

public class DataFlowRequestRepository {
    private static final String INSERT_TO_DATA_FLOW_REQUEST = "INSERT INTO data_flow_request (transaction_id, " +
            "data_flow_request) VALUES ($1, $2)";
    private static final String SELECT_HIP_ID_FROM_CONSENT_ARTEFACT = "SELECT consent_artefact -> 'hip' ->> 'id' as " +
            "hip_id FROM consent_artefact WHERE consent_artefact_id=$1";
    private static final String INSERT_TO_HEALTH_INFO_NOTIFICATION = "INSERT INTO health_info_notification " +
            "(transaction_id, notification_request) VALUES ($1, $2)";
    private final PgPool dbClient;

    public DataFlowRequestRepository(PgPool pgPool) {
        this.dbClient = pgPool;
    }

    public Mono<Void> addDataFlowRequest(String transactionId, DataFlowRequest dataFlowRequest) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_TO_DATA_FLOW_REQUEST)
                        .execute(
                                Tuple.of(transactionId, JsonObject.mapFrom(dataFlowRequest)),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new Exception("Failed to insert to data flow request"));
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }

    public Mono<String> getHipIdFor(String consentId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(SELECT_HIP_ID_FROM_CONSENT_ARTEFACT)
                        .execute(Tuple.of(consentId),
                                handler -> {
                                    if (handler.failed()) {
                                        monoSink.error(new Exception("Failed to get hip id from consent Id"));
                                        return;
                                    }
                                    monoSink.success(handler.result().iterator().next().getString(0));
                                }));
    }

    public Mono<Void> saveNotificationRequest(HealthInfoNotificationRequest notificationRequest) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_TO_HEALTH_INFO_NOTIFICATION)
                        .execute(Tuple.of(notificationRequest.getTransactionId(), JsonObject.mapFrom(notificationRequest)),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new Exception("Failed to insert to data flow notification"));
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
