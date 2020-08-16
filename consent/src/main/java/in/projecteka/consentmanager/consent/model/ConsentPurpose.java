package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ConsentPurpose implements Serializable {
    @NotEmpty(message = "Purpose reason is not specified.")
    private String text;

    @NotEmpty(message = "Purpose code is not specified.")
    private String code;
    private String refUri;
}
