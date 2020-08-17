package in.projecteka.user.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class KeyCloakError {
    private String error;
    @JsonProperty("error_description")
    private String errorDescription;
}
