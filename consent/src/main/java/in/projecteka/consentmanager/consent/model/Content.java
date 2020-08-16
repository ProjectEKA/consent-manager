package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Content {
    private String requester;
    private UUID consentRequestId;
    private String hiTypes;
    private String deepLinkUrl;
}
