package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class RecoverCmIdResponse {
    String cmId;
}