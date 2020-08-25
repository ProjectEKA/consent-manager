package in.projecteka.consentmanager.dataflow;

import static in.projecteka.consentmanager.Constants.CURRENT_VERSION;

public class Constants {
    public static final String PATH_HEALTH_INFORMATION_REQUEST = CURRENT_VERSION + "/health-information/request";
    public static final String PATH_HEALTH_INFORMATION_NOTIFY = CURRENT_VERSION + "/health-information/notify";
    public static final String PATH_HEALTH_INFORMATION_ON_REQUEST = CURRENT_VERSION + "/health-information/on-request";
    public static final String PATH_HEALTH_HIP_INFORMATION_REQUEST = "/health-information/hip/request";
    //GATEWAY URL PATHs
    public static final String PATH_DATA_FLOW_CM_ON_REQUEST = "/health-information/cm/on-request";

    private Constants() {

    }
}
