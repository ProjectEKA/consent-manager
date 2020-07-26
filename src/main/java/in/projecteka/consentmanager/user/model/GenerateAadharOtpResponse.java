package in.projecteka.consentmanager.user.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class GenerateAadharOtpResponse {
    @JsonAlias({"txnID","txnId"})
    private final String transactionId;
    private final String token;
}