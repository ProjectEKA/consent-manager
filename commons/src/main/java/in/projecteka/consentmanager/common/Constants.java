package in.projecteka.consentmanager.common;

import java.time.LocalDateTime;

public class Constants {
    public static final String API_VERSION = "v0.5";
    public static final String BLACKLIST = "blacklist";
    public static final String BLACKLIST_FORMAT = "%s:%s";
    public static final LocalDateTime DEFAULT_CACHE_VALUE = LocalDateTime.MIN;
    //APIs
    public static final String CURRENT_VERSION = "/" + API_VERSION;

    public static final String PATH_HEARTBEAT = CURRENT_VERSION + "/heartbeat";

    // Cache
    public static final String GUAVA = "guava";

    public static final String SERVICE_DOWN = "Service Down";

    private Constants() {
    }
}
