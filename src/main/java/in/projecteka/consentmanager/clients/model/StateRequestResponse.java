package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Value
public class StateRequestResponse {
    private List<DistrictData> districts;
    private String name;
    private String code;
}

