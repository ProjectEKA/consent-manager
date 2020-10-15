package in.projecteka.consentmanager.userauth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import javax.validation.constraints.NotBlank;

@Builder
@AllArgsConstructor
@Data
public class Requester {
    @NotBlank(message = "Requester type must be specified.")
    @NonNull
    private RequesterType type;

    @NonNull
    @NotBlank(message = "Requester id must be specified.")
    private String id;
}
