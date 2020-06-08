package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@Builder
public class RequesterDetail {
    @NotBlank(message = "Requester type must be specified.")
    private Requester type;
    @NotBlank(message = "Requester id must be specified.")
    private String id;
}