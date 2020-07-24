package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class HasOtpVerificationRequest {
    private String txnId;
    private String otp;
}
