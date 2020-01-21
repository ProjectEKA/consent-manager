package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentPermission {
    @NotNull(message = "Access mode is not specified.")
    private AccessMode accessMode;
    @Valid
    @NotNull(message = "Permission period is not specified.")
    private AccessPeriod dateRange;

    @NotNull(message = "Permission expiry is not specified.")
    @Future(message = "Permission expiry must be in future.")
    private Date dataExpiryAt;
    @Valid
    private DataFrequency frequency;

    public int comparePermissionDates() {
        return dateRange.getFromDate().compareTo(dateRange.getToDate());
    }
}
