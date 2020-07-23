package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccessPeriod implements Serializable {
    @JsonProperty("from")
    @NotNull(message = "From Date is not specified.")
    private LocalDateTime fromDate;

    @JsonProperty("to")
    @NotNull(message = "To Date is not specified.")
    private LocalDateTime toDate;
}
