package in.projecteka.dataflow;

import in.projecteka.dataflow.model.DataFlowRequestMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import java.util.List;

import static in.projecteka.library.common.Serializer.from;

@AllArgsConstructor
@Slf4j
public class PostDataFlowRequestApproval {
    private final SenderOptions senderOptions;
    private final DestinationInfo destinationInfo;

    @SneakyThrows
    public void broadcastDataFlowRequest(
            String transactionId,
            in.projecteka.dataflow.model.DataFlowRequest dataFlowRequest) {
        try (Sender sender = RabbitFlux.createSender(senderOptions)) {
            Flux<OutboundMessage> outboundFlux =
                    Flux.fromIterable(List.of(DataFlowRequestMessage.builder()
                            .transactionId(transactionId)
                            .dataFlowRequest(dataFlowRequest)
                            .build()))
                            .map(message -> {
                                String exchange = destinationInfo.getExchange();
                                String routingKey = destinationInfo.getRoutingKey();
                                return new OutboundMessage(exchange, routingKey, from(message).getBytes());
                            });
            sender.send(outboundFlux)
                    .doOnError(e -> log.error("Send failed", e))
                    .subscribe();
        }
    }
}
