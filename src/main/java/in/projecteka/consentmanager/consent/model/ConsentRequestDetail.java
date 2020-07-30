package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class ConsentRequestDetail {
    @JsonProperty("id")
    private String requestId;
    private ConsentStatus status;
    private LocalDateTime createdAt;
    private ConsentPurpose purpose;
    private PatientReference patient;
    private HIPReference hip;
    private HIUReference hiu;
    private Requester requester;
    private HIType[] hiTypes;
    private ConsentPermission permission;
    private String consentNotificationUrl;
    private LocalDateTime lastUpdated;
}
