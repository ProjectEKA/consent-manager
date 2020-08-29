package in.projecteka.dataflow;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class DestinationsConfig {
    private final Map<String, DestinationInfo> queues;

    @Getter
    @AllArgsConstructor
    public static class DestinationInfo {
        private final String exchange;
        private final String routingKey;
    }
}