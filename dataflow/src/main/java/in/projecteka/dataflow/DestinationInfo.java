package in.projecteka.dataflow;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DestinationInfo {
    private final String exchange;
    private final String routingKey;
}