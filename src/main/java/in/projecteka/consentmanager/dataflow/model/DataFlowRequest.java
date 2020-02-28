package in.projecteka.consentmanager.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFlowRequest {
    private Consent consent;
    private HIDataRange hiDataRange;
    private String callBackUrl;
    private KeyMaterial keyMaterial;

    public void setArtefactSignature(String signature) {
        consent.setDigitalSignature(signature);
    }
}
