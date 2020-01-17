package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ConsentPermission {
    private AccessMode accessMode;
    private AccessPeriod dateRange;
    private Date dataExpiryAt;
    private DataFrequency frequency;
}
