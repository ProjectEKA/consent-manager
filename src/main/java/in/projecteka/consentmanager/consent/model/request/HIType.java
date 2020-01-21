package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum HIType {

    CONDITION("Condition"),
    OBSERVATION("Observation"),
    DIAGNOSTIC_REPORT("DiagnosticReport"),
    MEDICATION_REQUEST("MedicationRequest");



    private final String resourceType;
    HIType(String value) {
        resourceType = value;
    }

    @JsonValue
    public String getValue() {
        return resourceType;
    }

    public HIType findByValue(String input) {
        return Arrays.stream(HIType.values())
                .filter(hiType -> hiType.resourceType == input)
                .findAny().get();
    }

}
