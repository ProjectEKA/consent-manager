package in.projecteka.consentmanager.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KeyMaterial {
    private String cryptoAlg;
    private String curve;
    private KeyStructure dhPublicKey;
    private KeyStructure randomKey;
}
