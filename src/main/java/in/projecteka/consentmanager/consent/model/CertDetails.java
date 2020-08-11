package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertDetails {
    private String kty;
    private String alg;
    private String use;
    private String n;
    private String e;
    private String fmt;
    private String publicKey;
    private String startDate;
}
