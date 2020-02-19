package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestMessage;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static DataFlowRequest.DataFlowRequestBuilder dataFlowRequest() {
        return easyRandom.nextObject(DataFlowRequest.DataFlowRequestBuilder.class);
    }

    public static ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder consentArtefactRepresentation() {
        return easyRandom.nextObject(ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder.class);
    }

    public static DataFlowRequestMessage.DataFlowRequestMessageBuilder dataFlowRequestMessage(){
        return easyRandom.nextObject(DataFlowRequestMessage.DataFlowRequestMessageBuilder.class);
    }

    public static in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest.DataFlowRequestBuilder dataFlowRequestBuilder(){
        return easyRandom.nextObject(in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest.DataFlowRequestBuilder.class);
    }
}
