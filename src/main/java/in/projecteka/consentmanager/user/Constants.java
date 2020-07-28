package in.projecteka.consentmanager.user;

import static in.projecteka.consentmanager.common.Constants.CURRENT_VERSION;

public class Constants {
    public static final String PATH_FIND_PATIENT = CURRENT_VERSION + "/patients/find";
    public static final String APP_PATH_CREATE_PIN = "/pin";
    public static final String APP_PATH_GET_PROFILE = "/me";
    public static final String APP_PATH_GET_PROFILE_LOGINMODE = "/profile/loginmode";
    public static final String APP_PATH_VERIFY_PIN = "/verify-pin";
    public static final String APP_PATH_GENERATE_OTP = "/generateotp";
    public static final String APP_PATH_VERIFY_OTP = "/verifyotp";
    public static final String APP_PATH_FORGET_PIN_GENERATE_OTP = "/forgot-pin/generate-otp";
    public static final String APP_PATH_FORGET_PIN_VALIDATE_OTP = "/forgot-pin/validate-otp";
    public static final String APP_PATH_FORGET_PIN_UPDATE_PIN = "/reset-pin";
    public static final String APP_PATH_UPDATE_PROFILE_PASSWORD = "/profile/update-password";
    public static final String APP_PATH_CHANGE_PIN = "/change-pin";
    public static final String APP_PATH_PROFILE_RECOVERY_INIT = "/profile/recovery-init";
    public static final String APP_PATH_PROFILE_RECOVERY_CONFIRM = "/profile/recovery-confirm";
    public static final String APP_PATH_NEW_SESSION = "/sessions";
    public static final String APP_PATH_VERIFY_OTP_FOR_SESSION = "/otpsession/verify";
    public static final String APP_PATH_SESSION_PERMIT_BY_OTP = "/otpsession/permit";
    public static final String APP_PATH_LOGOUT = "/logout";
    public static final String APP_PATH_FIND_BY_USER_NAME = "/users/{userName}";
    public static final String APP_PATH_USER_SIGN_UP_ENQUIRY = "/users/verify";
    public static final String APP_PATH_INTERNAL_FIND_USER_BY_USERNAME = "/internal/users/{userName}";
    public static final String APP_PATH_INTERNAL_GET_CARE_CONTEXT = "/internal/users/care-contexts";
}
