package in.projecteka.dataflow;


import static in.projecteka.library.common.Constants.CURRENT_VERSION;

public class Constants {
    public static final String PATH_HEALTH_INFORMATION_REQUEST = CURRENT_VERSION + "/health-information/request";
    public static final String PATH_HEALTH_INFORMATION_NOTIFY = CURRENT_VERSION + "/health-information/notify";
    public static final String PATH_HEALTH_INFORMATION_ON_REQUEST = CURRENT_VERSION + "/health-information/on-request";
    public static final String PATH_HEALTH_HIP_INFORMATION_REQUEST = "/health-information/hip/request";
    public static final String HIP_DATA_FLOW_REQUEST_QUEUE = "hip-data-flow-request-queue";
    public static final String EXCHANGE = "exchange";

    //GATEWAY URL PATHs
    public static final String PATH_DATA_FLOW_CM_ON_REQUEST = "/health-information/cm/on-request";

    //Headers
    public static final String HDR_HIP_ID = "X-HIP-ID";
    public static final String HDR_HIU_ID = "X-HIU-ID";
}
