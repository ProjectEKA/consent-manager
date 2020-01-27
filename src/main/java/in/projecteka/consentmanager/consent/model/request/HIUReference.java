package in.projecteka.consentmanager.consent.model.request;

import lombok.*;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HIUReference {
    @NotEmpty(message = "HIU identifier is not specified.")
    private String id;
    private String name;
}
