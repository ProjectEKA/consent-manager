package in.projecteka.consentmanager.link.discovery.model.patient.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class PatientRequest {
    private final Patient patient;
    private final String requestId;
}
