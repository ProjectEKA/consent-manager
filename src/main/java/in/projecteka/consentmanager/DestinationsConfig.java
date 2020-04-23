package in.projecteka.consentmanager;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class DestinationsConfig {
    private Map<String, DestinationInfo> queues;
    private Map<String, DestinationInfo> topics;

    @Getter
    @AllArgsConstructor
    public static class DestinationInfo {
        private String exchange;
        private String routingKey;
    }
}