package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HIPReference implements Serializable {
    @NotEmpty(message = "HIP identifier is not specified.")
    private String id;
    @JsonInclude(Include.NON_NULL)
    private String name;
}
