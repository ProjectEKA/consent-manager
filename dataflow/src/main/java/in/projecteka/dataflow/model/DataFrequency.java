package in.projecteka.dataflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class DataFrequency {
    private DataFrequencyUnit unit;
    private int value;
    private int repeats;
}
