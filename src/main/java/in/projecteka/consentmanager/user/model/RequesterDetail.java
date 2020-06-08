package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RequesterDetail {
    @NotBlank(message = "Requester type must be specified.")
    private String type;
    @NotBlank(message = "Requester id must be specified.")
    private String id;
}