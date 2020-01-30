package in.projecteka.consentmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@AllArgsConstructor
@ConfigurationProperties("destinations")
public class DestinationsConfig {
    private Map<String, DestinationInfo> queues;
    private Map<String, DestinationInfo> topics;

    @Data
    @AllArgsConstructor
    public static class DestinationInfo {
        private String exchange;
        private String routingKey;
    }
}