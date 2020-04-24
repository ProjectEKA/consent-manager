package in.projecteka.consentmanager.dataflow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@Getter
@ConfigurationProperties(prefix = "consentmanager.dataflow.consentmanager")
@AllArgsConstructor
public class DataFlowConsentManagerProperties {
    private final String url;
}