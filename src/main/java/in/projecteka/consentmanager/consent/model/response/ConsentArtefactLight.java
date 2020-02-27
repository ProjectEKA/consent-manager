package in.projecteka.consentmanager.consent.model.response;

import in.projecteka.consentmanager.consent.model.ConsentPermission;
import in.projecteka.consentmanager.consent.model.HIUReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentArtefactLight {
    private HIUReference hiu;
    private ConsentPermission permission;
}
