package in.projecteka.consentmanager.dataflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentPurpose {
    private String text;
    private String code;
    private String refUri;
}
