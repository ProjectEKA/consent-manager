package in.projecteka.library.common;

import java.time.LocalDateTime;

public class Constants {
    public static final String API_VERSION = "v0.5";
    public static final String BLACKLIST = "blacklist";
    public static final String BLACKLIST_FORMAT = "%s:%s";
    public static final LocalDateTime DEFAULT_CACHE_VALUE = LocalDateTime.MIN;
    public static final String SCOPE_CONSENT_APPROVE = "consentrequest.approve";
    public static final String SCOPE_CONSENT_REVOKE = "consent.revoke";
    public static final String SCOPE_CHANGE_PIN = "profile.changepin";
    public static final String CORRELATION_ID = "CORRELATION-ID";


    //APIs
    public static final String CURRENT_VERSION = "/" + API_VERSION;

    public static final String PATH_HEARTBEAT = CURRENT_VERSION + "/heartbeat";

    // Cache
    public static final String GUAVA = "guava";

    public static final String SERVICE_DOWN = "Service Down";

    private Constants() {
    }
}
