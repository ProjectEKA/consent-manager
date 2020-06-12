package in.projecteka.consentmanager.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class ServiceCaller {
    String clientId;
    Role role;
}
