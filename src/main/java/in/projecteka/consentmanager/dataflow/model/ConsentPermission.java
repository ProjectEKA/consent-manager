package in.projecteka.consentmanager.dataflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentPermission {
    private AccessMode accessMode;
    private AccessPeriod dateRange;
    private Date dataEraseAt;
    private DataFrequency frequency;
}
