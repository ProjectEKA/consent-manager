package in.projecteka.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentArtefact {
    private HIUReference hiu;
    private HIPReference hip;
    private ConsentPermission permission;
}
