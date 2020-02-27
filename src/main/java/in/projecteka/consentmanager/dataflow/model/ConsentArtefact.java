package in.projecteka.consentmanager.dataflow.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentArtefact {
    private HIUReference hiu;
    private ConsentPermission permission;
}
