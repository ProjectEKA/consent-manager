package in.projecteka.consentmanager.link;

import static in.projecteka.consentmanager.common.Constants.CURRENT_VERSION;

public class Constants {
    public static final String APP_PATH_CONFIRM_LINK = "/v1/links/link/confirm";
    public static final String APP_PATH_CONFIRM_LINK_REF_NUMBER = APP_PATH_CONFIRM_LINK + "/{linkRefNumber}";
    public static final String APP_PATH_CARE_CONTEXTS_DISCOVER = "/v1/care-contexts/discover";
    public static final String APP_PATH_INTERNAL_GET_LINKED_CARE_CONTEXTS = "internal/patients/{username}/links";
    public static final String APP_PATH_LINK_INIT = "/v1/links/link/init";
    public static final String APP_PATH_GET_PATIENTS_LINKS = "/patients/links";

    //The following are Gateway facing APIs
    public static final String PATH_CARE_CONTEXTS_ON_DISCOVER = CURRENT_VERSION + "/care-contexts/on-discover";
    public static final String PATH_LINK_ON_INIT = CURRENT_VERSION + "/links/link/on-init";
    public static final String PATH_LINK_ON_CONFIRM = CURRENT_VERSION + "/links/link/on-confirm";

    private Constants() {

    }
}
