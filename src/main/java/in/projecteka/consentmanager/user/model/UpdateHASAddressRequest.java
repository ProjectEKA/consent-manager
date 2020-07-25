package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class UpdateHASAddressRequest {
    private final String healthId;
    private final String districtCode;
    private final String stateCode;
}
