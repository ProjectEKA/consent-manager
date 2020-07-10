package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.ErrorCode;

import java.util.HashMap;
import java.util.Map;

public class ErrorMap {

    private static final Map<Integer, ErrorCode> errorMap = new HashMap<>() {
        {
            //hip
            put(3404, ErrorCode.NO_PATIENT_FOUND);
            put(3402, ErrorCode.CARE_CONTEXT_NOT_FOUND);
            put(3405, ErrorCode.OTP_INVALID);
            put(3406, ErrorCode.OTP_EXPIRED);
            put(3413, ErrorCode.INVALID_LINK_REFERENCE);
            put(3507, ErrorCode.FAILED_TO_GET_LINKED_CARE_CONTEXTS);
            put(3409, ErrorCode.DUPLICATE_DISCOVERY_REQUEST);
            put(3407, ErrorCode.DISCOVERY_REQUEST_NOT_FOUND);

            //gateway
            put(2500, ErrorCode.UNKNOWN_ERROR_OCCURRED);
        }

    };

    public static ErrorCode toCmError(Integer errorCode) {
        ErrorCode cmErrorCode = errorMap.get(errorCode);
        if (cmErrorCode == null) {
            return ErrorCode.UNKNOWN_ERROR_OCCURRED;
        }
        return cmErrorCode;
    }
}
