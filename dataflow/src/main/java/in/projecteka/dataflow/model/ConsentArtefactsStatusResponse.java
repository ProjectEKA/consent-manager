package in.projecteka.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ConsentArtefactsStatusResponse {
    private final String status;
}
