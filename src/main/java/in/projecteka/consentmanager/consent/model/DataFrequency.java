package in.projecteka.consentmanager.consent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class DataFrequency implements Serializable {
    @NotNull(message = "Frequency unit is not specified.")
    private DataFrequencyUnit unit;

    @Positive(message = "Frequency value is not valid.")
    private int value;

    @Min(value = 0, message = "Frequency repeat is not valid.")
    private int repeats;
}
