package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsentPermission implements Serializable {
    @NotNull(message = "Access mode is not specified.")
    private AccessMode accessMode;

    @Valid
    @NotNull(message = "Permission period is not specified.")
    private AccessPeriod dateRange;

    @NotNull(message = "Permission expiry is not specified.")
    @Future(message = "Permission expiry must be in future.")
    private Date dataEraseAt;

    @Valid
    private DataFrequency frequency;

    public int comparePermissionDates() {
        return dateRange.getFromDate().compareTo(dateRange.getToDate());
    }
}
