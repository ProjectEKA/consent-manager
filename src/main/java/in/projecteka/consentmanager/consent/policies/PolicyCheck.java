package in.projecteka.consentmanager.consent.policies;

public interface PolicyCheck<T> {
    void checkPolicyFor(T consent);
}
