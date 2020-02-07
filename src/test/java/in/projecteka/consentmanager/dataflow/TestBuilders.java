package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static DataFlowRequest.DataFlowRequestBuilder dataFlowRequest() {
        return easyRandom.nextObject(DataFlowRequest.DataFlowRequestBuilder.class);
    }

    public static ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder consentArtefactRepresentation() {
        return easyRandom.nextObject(ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder.class);
    }

    public static DataFlowRequestResponse.DataFlowRequestResponseBuilder dataFlowRequestRespone() {
        return easyRandom.nextObject(DataFlowRequestResponse.DataFlowRequestResponseBuilder.class);
    }
}
