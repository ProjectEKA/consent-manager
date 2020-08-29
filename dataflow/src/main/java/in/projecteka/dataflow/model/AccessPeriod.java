package in.projecteka.dataflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@AllArgsConstructor
@Builder
public class AccessPeriod {
    @JsonProperty("from")
    @NotNull(message = "From Date is not specified.")
    private final LocalDateTime fromDate;

    @JsonProperty("to")
    @NotNull(message = "To Date is not specified.")
    private final LocalDateTime toDate;
}
