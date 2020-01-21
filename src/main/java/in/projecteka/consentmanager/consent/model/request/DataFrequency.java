package in.projecteka.consentmanager.consent.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class DataFrequency {
    @NotNull(message = "Frequency unit is not specified.")
    private DataFrequencyUnit unit;

    @Positive(message = "Frequency value is not valid.")
    private int value;

    @Min(value = 0, message = "Frequency repeat is not valid.")
    private int repeats;
}
