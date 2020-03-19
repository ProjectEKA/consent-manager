package in.projecteka.consentmanager.dataflow.model;

import com.fasterxml.jackson.annotation.JsonValue;

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
}
