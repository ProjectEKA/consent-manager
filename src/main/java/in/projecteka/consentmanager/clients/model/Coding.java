package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Coding {
    private String code;
}
