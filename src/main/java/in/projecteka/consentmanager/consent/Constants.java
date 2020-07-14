package in.projecteka.consentmanager.consent;

import static in.projecteka.consentmanager.common.Constants.CURRENT_VERSION;

public class Constants {
    public static final String PATH_CONSENT_REQUESTS_INIT = CURRENT_VERSION + "/consent-requests/init";
    public static final String PATH_CONSENTS_FETCH = CURRENT_VERSION + "/consents/fetch";
    public static final String PATH_HIP_CONSENT_ON_NOTIFY = CURRENT_VERSION + "/consents/hip/on-notify";
    public static final String APP_PATH_GET_CONSENT_ARTEFACTS = "/consent-artefacts";
    public static final String APP_PATH_REVOKE_CONSENTS = "/consents/revoke";
    public static final String APP_PATH_INTERNAL_GET_CONSENT = "/internal/consents/{consentId}";
    public static final String APP_PATH_GET_CONSENT = "/consents/{consentId}";
    public static final String APP_PATH_APPROVE_CONSENT_REQUEST = "/consent-requests/{request-id}/approve";
    public static final String APP_PATH_DENY_CONSENT = "/consent-requests/{id}/deny";
    public static final String APP_PATH_GET_CONSENT_REQUESTS = "/consent-requests";
    public static final String APP_PATH_GET_CONSENT_ARTEFACTS_FOR_REQUEST = "/consent-requests/{request-id}/consent-artefacts";
}
