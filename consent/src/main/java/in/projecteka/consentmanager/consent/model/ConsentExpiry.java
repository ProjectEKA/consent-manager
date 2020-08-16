package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentExpiry {
    private String consentId;
    private LocalDateTime consentExpiryDate;
    private String patientId;
}
