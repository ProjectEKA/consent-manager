package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccessPeriod {
    @JsonProperty("from")
    @NotNull(message = "From Date is not specified.")
    private Date fromDate;

    @JsonProperty("to")
    @NotNull(message = "To Date is not specified.")
    private Date toDate;
}
