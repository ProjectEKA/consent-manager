package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum HIType {

    CONDITION("Condition"),
    OBSERVATION("Observation"),
    DIAGNOSTIC_REPORT("DiagnosticReport"),
    MEDICATION_REQUEST("MedicationRequest"),
    DOCUMENT_REFERENCE("DocumentReference"),
    PRESCRIPTION("Prescription"),
    DISCHARGE_SUMMARY("DischargeSummary");

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
                .filter(hiType -> hiType.resourceType.equals(input))
                .findAny().get();
    }

}
