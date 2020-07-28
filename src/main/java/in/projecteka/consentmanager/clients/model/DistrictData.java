package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Value
public class DistrictData {
    private String code;
    private String name;
}
