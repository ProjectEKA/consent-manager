package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.ErrorCode;

import java.util.HashMap;
import java.util.Map;

public class ErrorMap {

    private static final Map<Integer, ErrorCode> hipCmErrorMap = new HashMap<> () {
            {
                //link
                put(1000, ErrorCode.NO_PATIENT_FOUND);
                put(1002, ErrorCode.CARE_CONTEXT_NOT_FOUND);
                put(1003, ErrorCode.OTP_INVALID);
                put(1004, ErrorCode.OTP_EXPIRED);
                put(1006, ErrorCode.INVALID_LINK_REFERENCE);
                put(1014, ErrorCode.FAILED_TO_GET_LINKED_CARE_CONTEXTS); //FailedToGetLinkedCareContexts
                put(1015, ErrorCode.DUPLICATE_DISCOVERY_REQUEST); //DuplicateDiscoveryRequest
            }
    };

    public static ErrorCode hipToCmError(Integer hipErrorCode) {
        ErrorCode cmErrorCode = hipCmErrorMap.get(hipErrorCode);
        if (cmErrorCode == null) {
            return ErrorCode.UNKNOWN_ERROR_OCCURRED;
        }
        return cmErrorCode;
    }
}
