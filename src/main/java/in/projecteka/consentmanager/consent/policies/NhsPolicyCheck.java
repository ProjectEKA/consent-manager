package in.projecteka.consentmanager.consent.policies;

import in.projecteka.consentmanager.consent.model.ConsentRequest;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NhsPolicyCheck implements PolicyCheck<ConsentRequest> {
    @Override
    public boolean checkPolicyFor(ConsentRequest consent, String hiuId) {
        return (consent.getDetail().getPatient().getId().equals(consent.getDetail().getRequester().getName()) &&
                consent.getDetail().getHiu().getId().equals(hiuId));
    }
}
