package in.projecteka.consentmanager.dataflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class GrantedContext implements Serializable {
    private String patientReference;
    private String careContextReference;
}
