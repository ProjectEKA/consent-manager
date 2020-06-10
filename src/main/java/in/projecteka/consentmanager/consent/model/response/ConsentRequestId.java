package in.projecteka.consentmanager.consent.model.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class ConsentRequestId {
    UUID id;
}
