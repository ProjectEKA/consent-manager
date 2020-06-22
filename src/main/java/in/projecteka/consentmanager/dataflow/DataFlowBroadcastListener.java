package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.MessageListenerContainerFactory;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.DataRequestNotifier;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestMessage;
import in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.hip.DataRequest;
import in.projecteka.consentmanager.dataflow.model.hip.HiRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.UUID;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIP_DATA_FLOW_REQUEST_QUEUE;

@AllArgsConstructor
public class DataFlowBroadcastListener {
    private static final Logger logger = LoggerFactory.getLogger(DataFlowBroadcastListener.class);
    private final MessageListenerContainerFactory messageListenerContainerFactory;
    private final DestinationsConfig destinationsConfig;
    private final Jackson2JsonMessageConverter converter;
    private final DataRequestNotifier dataRequestNotifier;
    private final DataFlowRequestRepository dataFlowRequestRepository;
    private final CentralRegistry clientRegistryClient;
    private final GatewayServiceProperties gatewayServiceProperties;

    @PostConstruct
    public void subscribe() throws ClientError {
        DestinationsConfig.DestinationInfo destinationInfo = destinationsConfig
                .getQueues()
                .get(HIP_DATA_FLOW_REQUEST_QUEUE);

        MessageListenerContainer mlc = messageListenerContainerFactory
                .createMessageListenerContainer(destinationInfo.getRoutingKey());

        MessageListener messageListener = message -> {
            try {
                DataFlowRequestMessage dataFlowRequestMessage =
                        (DataFlowRequestMessage) converter.fromMessage(message);
                logger.info("Received message for Request id : {}", dataFlowRequestMessage
                        .getTransactionId());
                var dataFlowRequest = dataFlowRequestMessage.getDataFlowRequest();
                if(gatewayServiceProperties.getEnabled()) {
                    DataRequest dataRequest = DataRequest.builder()
                            .transactionId(UUID.fromString(dataFlowRequestMessage.getTransactionId()))
                            .requestId(UUID.randomUUID())
                            .timestamp(LocalDateTime.now())
                            .hiRequest(HiRequest.builder()
                                    .consent(dataFlowRequest.getConsent())
                                    .dataPushUrl(dataFlowRequest.getDataPushUrl())
                                    .dateRange(dataFlowRequest.getDateRange())
                                    .keyMaterial(dataFlowRequest.getKeyMaterial())
                                    .build()
                            ).build();
                    configureAndSendDataRequestFor(dataRequest);
                } else {
                    DataFlowRequest dataRequest = DataFlowRequest.builder()
                            .transactionId(dataFlowRequestMessage.getTransactionId())
                            .dataPushUrl(dataFlowRequest.getDataPushUrl())
                            .consent(dataFlowRequest.getConsent())
                            .dateRange(dataFlowRequest.getDateRange())
                            .keyMaterial(dataFlowRequest.getKeyMaterial())
                            .build();
                    configureAndSendDataRequestFor(dataRequest);
                }
            } catch (Exception e) {
                throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
            }
        };
        mlc.setupMessageListener(messageListener);
        mlc.start();
    }

    public void configureAndSendDataRequestFor(DataFlowRequest dataFlowRequest) {
        dataFlowRequestRepository.getHipIdFor(dataFlowRequest.getConsent().getId())
                .flatMap(this::providerUrl)
                .flatMap(url -> dataRequestNotifier.notifyHip(dataFlowRequest, url))
                .block();
    }

    public void configureAndSendDataRequestFor(DataRequest dataFlowRequest) {
        dataFlowRequestRepository.getHipIdFor(dataFlowRequest.getHiRequest().getConsent().getId())
                .flatMap(hipId -> dataRequestNotifier.notifyHip(dataFlowRequest, hipId))
                .block();
    }

    private Mono<String> providerUrl(String providerId) {
        return clientRegistryClient.providerWith(providerId)
                .flatMap(provider ->
                        provider.getIdentifiers()
                                .stream()
                                .filter(Identifier::isOfficial)
                                .findFirst()
                                .map(identifier -> Mono.just(identifier.getSystem()))
                                .orElse(Mono.empty()))
                .flatMap(url -> {
                    if (url == null) {
                        logger.error("Hip Url not found for Hip Id");
                        return Mono.empty();
                    } else {
                        return Mono.just(url);
                    }
                });
    }
}
