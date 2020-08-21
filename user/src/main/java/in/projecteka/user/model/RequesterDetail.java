package in.projecteka.user.model;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@Builder
public class RequesterDetail {
    @NotBlank(message = "Requester type must be specified.")
    Requester type;
    @NotBlank(message = "Requester id must be specified.")
    String id;
}