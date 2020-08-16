package in.projecteka.library.clients;

import in.projecteka.library.clients.model.ErrorCode;

import java.util.Map;

import static in.projecteka.library.clients.model.ErrorCode.CARE_CONTEXT_NOT_FOUND;
import static in.projecteka.library.clients.model.ErrorCode.DISCOVERY_REQUEST_NOT_FOUND;
import static in.projecteka.library.clients.model.ErrorCode.DUPLICATE_DISCOVERY_REQUEST;
import static in.projecteka.library.clients.model.ErrorCode.FAILED_TO_GET_LINKED_CARE_CONTEXTS;
import static in.projecteka.library.clients.model.ErrorCode.INVALID_LINK_REFERENCE;
import static in.projecteka.library.clients.model.ErrorCode.OTP_EXPIRED;
import static in.projecteka.library.clients.model.ErrorCode.OTP_INVALID;
import static in.projecteka.library.clients.model.ErrorCode.UNKNOWN_ERROR_OCCURRED;
import static java.util.Map.of;
import static java.util.Objects.isNull;

public class ErrorMap {

    //hip
    private static final Map<Integer, ErrorCode> ERROR_CODE_MAP = of(3404, ErrorCode.NO_PATIENT_FOUND,
            3402, CARE_CONTEXT_NOT_FOUND,
            3405, OTP_INVALID,
            3406, OTP_EXPIRED,
            3413, INVALID_LINK_REFERENCE,
            3507, FAILED_TO_GET_LINKED_CARE_CONTEXTS,
            3409, DUPLICATE_DISCOVERY_REQUEST,
            3407, DISCOVERY_REQUEST_NOT_FOUND,
            //gateway
            2500, UNKNOWN_ERROR_OCCURRED);

    public static ErrorCode toCmError(Integer errorCode) {
        return isNull(errorCode)
               ? UNKNOWN_ERROR_OCCURRED
               : ERROR_CODE_MAP.getOrDefault(errorCode, UNKNOWN_ERROR_OCCURRED);
    }

    private ErrorMap() {
    }
}
