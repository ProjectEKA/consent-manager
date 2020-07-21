package in.projecteka.consentmanager.common;

public class Constants {
    public static final String API_VERSION = "v0.5";
    public static final String BLACKLIST = "blacklist";
    public static final String BLACKLIST_FORMAT = "%s:%s";
    public static final String SCOPE_CONSENT_APPROVE = "consentrequest.approve";
    public static final String SCOPE_CONSENT_REVOKE = "consent.revoke";
    public static final String SCOPE_CHANGE_PIN = "profile.changepin";

    // rabbitmq
    public static final String CONSENT_REQUEST_QUEUE = "consent-request-queue";
    public static final String HIP_CONSENT_NOTIFICATION_QUEUE = "hip-consent-notification-queue";
    public static final String HIU_CONSENT_NOTIFICATION_QUEUE = "hiu-consent-notification-queue";
    public static final String HIP_DATA_FLOW_REQUEST_QUEUE = "hip-data-flow-request-queue";
    public static final String PARKING_EXCHANGE = "parking.exchange";
    public static final String EXCHANGE = "exchange";

    //Headers
    public static final String HDR_HIP_ID = "X-HIP-ID";
    public static final String HDR_HIU_ID = "X-HIU-ID";

    //APIs
    public static final String CURRENT_VERSION = "/" + API_VERSION;

    public static final String PATH_HEARTBEAT = CURRENT_VERSION + "/heartbeat";

    //Gateway API paths
    public static final String PATIENTS_CARE_CONTEXTS_LINK_CONFIRMATION_URL_PATH = "%s/links/link/confirm";
    public static final String PATIENTS_CARE_CONTEXTS_LINK_INIT_URL_PATH = "%s/links/link/init";

    private Constants() {
    }
}
