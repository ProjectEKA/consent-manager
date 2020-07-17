package in.projecteka.consentmanager.consent.policies;

public interface PolicyCheck<T> {
    boolean checkPolicyFor(T consent, String hiuId);
}
