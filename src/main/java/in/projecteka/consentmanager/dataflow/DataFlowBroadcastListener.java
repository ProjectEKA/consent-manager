package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.DataRequestNotifier;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestMessage;
import in.projecteka.consentmanager.dataflow.model.hip.DataRequest;
import in.projecteka.consentmanager.dataflow.model.hip.HiRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static in.projecteka.consentmanager.common.Constants.HIP_DATA_FLOW_REQUEST_QUEUE;

@AllArgsConstructor
public class DataFlowBroadcastListener {
    private static final Logger logger = LoggerFactory.getLogger(DataFlowBroadcastListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final Jackson2JsonMessageConverter converter;
    private final DataRequestNotifier dataRequestNotifier;
    private final DataFlowRequestRepository dataFlowRequestRepository;

    @PostConstruct
    public void subscribe() {

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
                throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
            }
        };
        mlc.setupMessageListener(messageListener);
        mlc.start();
    }

    public void configureAndSendDataRequestFor(DataRequest dataFlowRequest) {
        dataFlowRequestRepository.getHipIdFor(dataFlowRequest.getHiRequest().getConsent().getId())
                .flatMap(hipId -> dataRequestNotifier.notifyHip(dataFlowRequest, hipId))
                .block();
    }
}
