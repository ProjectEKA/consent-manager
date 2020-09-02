package in.projecteka.dataflow;

import in.projecteka.dataflow.model.DataFlowRequestMessage;
import in.projecteka.dataflow.model.hip.DataRequest;
import in.projecteka.dataflow.model.hip.HiRequest;
import in.projecteka.library.clients.model.ClientError;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static in.projecteka.dataflow.Constants.HIP_DATA_FLOW_REQUEST_QUEUE;
import static in.projecteka.dataflow.model.HipConsentArtefactNotificationStatus.*;

@AllArgsConstructor
public class DataFlowBroadcastListener {
    private static final Logger logger = LoggerFactory.getLogger(DataFlowBroadcastListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final Jackson2JsonMessageConverter converter;
    private final DataRequestNotifier dataRequestNotifier;
    private final ConsentManagerClient consentManagerClient;

    @PostConstruct
    public void subscribe() {

        logger.info("Listener initiated");
        var mlc = messageListenerContainerFactory.createMessageListenerContainer(HIP_DATA_FLOW_REQUEST_QUEUE);

        MessageListener messageListener = message -> {
            try {
                DataFlowRequestMessage dataFlowRequestMessage =
                        (DataFlowRequestMessage) converter.fromMessage(message);
                logger.info("Received message for Request id : {}", dataFlowRequestMessage
                        .getTransactionId());
                var dataFlowRequest = dataFlowRequestMessage.getDataFlowRequest();
                DataRequest dataRequest = DataRequest.builder()
                        .transactionId(UUID.fromString(dataFlowRequestMessage.getTransactionId()))
                        .requestId(UUID.randomUUID())
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .hiRequest(HiRequest.builder()
                                .consent(dataFlowRequest.getConsent())
                                .dataPushUrl(dataFlowRequest.getDataPushUrl())
                                .dateRange(dataFlowRequest.getDateRange())
                                .keyMaterial(dataFlowRequest.getKeyMaterial())
                                .build())
                        .build();
                configureAndSendDataRequestFor(dataRequest);
            } catch (Exception e) {
                logger.error("Error happened while sending {}", e.getMessage(), e);
                throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
            }
        };
        mlc.setupMessageListener(messageListener);
        mlc.start();
    }

    public void configureAndSendDataRequestFor(DataRequest dataFlowRequest) {
        String consentId = dataFlowRequest.getHiRequest().getConsent().getId();
        consentManagerClient.getConsentArtefact(consentId)
                .flatMap(caRep ->
                        consentManagerClient.getConsentArtefactStatus(consentId)
                                .flatMap(status -> status.getStatus().equals(NOTIFIED.toString()) ?
                                        dataRequestNotifier.notifyHip(
                                                dataFlowRequest, caRep.getConsentDetail().getHip().getId()) :
                                        Mono.error(ClientError.consentArtefactsYetToReachHIP())))
                .block();
    }
}
