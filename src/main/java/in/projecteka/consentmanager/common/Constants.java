package in.projecteka.consentmanager.common;

public class Constants {
    public static final String API_VERSION = "v1";
    public static final String BLACKLIST = "blacklist";
    public static final String BLACKLIST_FORMAT = "%s:%s";
    public static final String SCOPE_CONSENT_APPROVE = "consentrequest.approve";
    public static final String SCOPE_CONSENT_REVOKE = "consent.revoke";
    public static final String SCOPE_CHANGE_PIN = "profile.changepin";

    //Headers
    public static final String HDR_HIP_ID = "X-HIP-ID";
    public static final String HDR_HIU_ID = "X-HIU-ID";

    //APIs
    public static final String V_1_CARE_CONTEXTS_ON_DISCOVER = "/v1/care-contexts/on-discover";
    public static final String V_1_CONSENT_REQUESTS_INIT = "/v1/consent-requests/init";
    public static final String V_1_CONSENTS_FETCH = "/v1/consents/fetch";
    public static final String V_1_PATIENTS_FIND = "/v1/patients/find";
    public static final String V_1_LINKS_LINK_ON_INIT = "/v1/links/link/on-init";
    public static final String V_1_LINKS_LINK_ON_CONFIRM = "/v1/links/link/on-confirm";
    public static final String V_1_HEALTH_INFORMATION_REQUEST = "/v1/health-information/request";

    //GATEWAY URL PATHs
    public static final String DATA_FLOW_REQUEST_URL_PATH = "/health-information/cm/on-request";


    private Constants() {}
}
