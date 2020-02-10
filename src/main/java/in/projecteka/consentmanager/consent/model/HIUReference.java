package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HIUReference implements Serializable {
    @NotEmpty(message = "HIU identifier is not specified.")
    private String id;
    private String name;
}
