package in.projecteka.dataflow;

import in.projecteka.dataflow.model.DataFlowRequestMessage;
import in.projecteka.dataflow.model.hip.DataRequest;
import in.projecteka.dataflow.model.hip.HiRequest;
import in.projecteka.library.common.Serializer;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static in.projecteka.dataflow.Constants.HIP_DATA_FLOW_REQUEST_QUEUE;

@AllArgsConstructor
public class DataFlowBroadcastListener {
    private static final Logger logger = LoggerFactory.getLogger(DataFlowBroadcastListener.class);
    private final ReceiverOptions receiverOptions;
    private final DataRequestNotifier dataRequestNotifier;
    private final ConsentManagerClient consentManagerClient;

    @PostConstruct
    public void subscribe() {
        try (Receiver receiver = RabbitFlux.createReceiver(receiverOptions)) {
            receiver.consumeAutoAck(HIP_DATA_FLOW_REQUEST_QUEUE)
                    .map(delivery -> Serializer.to(delivery.getBody(), DataFlowRequestMessage.class))
                    .flatMap(message -> {
                        logger.info("Received message for Request id : {}", message
                                .getTransactionId());
                        var dataFlowRequest = message.getDataFlowRequest();
                        DataRequest dataRequest = DataRequest.builder()
                                .transactionId(UUID.fromString(message.getTransactionId()))
                                .requestId(UUID.randomUUID())
                                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                                .hiRequest(HiRequest.builder()
                                        .consent(dataFlowRequest.getConsent())
                                        .dataPushUrl(dataFlowRequest.getDataPushUrl())
                                        .dateRange(dataFlowRequest.getDateRange())
                                        .keyMaterial(dataFlowRequest.getKeyMaterial())
                                        .build())
                                .build();
                        return configureAndSendDataRequestFor(dataRequest);
                    })
                    .subscribe();
        }
    }

    public Mono<?> configureAndSendDataRequestFor(DataRequest dataFlowRequest) {
        return consentManagerClient.getConsentArtefact(dataFlowRequest.getHiRequest().getConsent().getId())
                .flatMap(caRep -> dataRequestNotifier.notifyHip(dataFlowRequest,
                        caRep.getConsentDetail().getHip().getId()));
    }
}
