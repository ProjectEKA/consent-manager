package in.projecteka.consentmanager.consent.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ConsentRequestResponse {

    @JsonProperty("id")
    private String consentRequestId;


    public static ConsentRequestResponseBuilder builder() {
        return new ConsentRequestResponseBuilder();
    }


    public static class ConsentRequestResponseBuilder {
        private String requestId;
        public ConsentRequestResponseBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        public ConsentRequestResponse build() {
            ConsentRequestResponse response = new ConsentRequestResponse();
            response.consentRequestId = requestId;
            return response;
        }
    }


}
