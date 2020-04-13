package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.ConsentManagerConfiguration.HIP_DATA_FLOW_REQUEST_QUEUE;
import static in.projecteka.consentmanager.clients.ClientError.queueNotFound;

@AllArgsConstructor
@Slf4j
public class PostDataFlowRequestApproval {
    private AmqpTemplate amqpTemplate;
    private DestinationsConfig destinationsConfig;

    @SneakyThrows
    public Mono<Void> broadcastDataFlowRequest(
            String transactionId,
            in.projecteka.consentmanager.dataflow.model.DataFlowRequest dataFlowRequest) {
        DestinationsConfig.DestinationInfo destinationInfo =
                destinationsConfig.getQueues().get(HIP_DATA_FLOW_REQUEST_QUEUE);
        if (destinationInfo == null) {
            log.info(HIP_DATA_FLOW_REQUEST_QUEUE + " not found");
            throw queueNotFound();
        }
        return Mono.create(monoSink -> {
            amqpTemplate.convertAndSend(
                    destinationInfo.getExchange(),
                    destinationInfo.getRoutingKey(),
                    DataFlowRequestMessage.builder()
                            .transactionId(transactionId)
                            .dataFlowRequest(dataFlowRequest)
                            .build());
            log.info("Broadcasting data flow request with transaction id : " + transactionId);
            monoSink.success();
        });
    }
}
