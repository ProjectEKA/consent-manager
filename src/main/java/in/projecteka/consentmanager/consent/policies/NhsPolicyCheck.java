package in.projecteka.consentmanager.consent.policies;

import in.projecteka.consentmanager.consent.model.ConsentRequest;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NhsPolicyCheck implements PolicyCheck<ConsentRequest> {
    @Override
    public void checkPolicyFor(ConsentRequest consent) {

    }
}
