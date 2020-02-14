package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.dataflow.model.AccessPeriod;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefact;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.ConsentPermission;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestMessage;
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

    public static DataFlowRequestMessage.DataFlowRequestMessageBuilder dataFlowRequestMessage(){
        return easyRandom.nextObject(DataFlowRequestMessage.DataFlowRequestMessageBuilder.class);
    }

    public static in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest.DataFlowRequestBuilder dataFlowRequestBuilder(){
        return easyRandom.nextObject(in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest.DataFlowRequestBuilder.class);
    }

    public static ConsentArtefact.ConsentArtefactBuilder consentArtefact() {
        return easyRandom.nextObject(ConsentArtefact.ConsentArtefactBuilder.class);
    }

    public static ConsentPermission.ConsentPermissionBuilder consentPermission() {
        return easyRandom.nextObject(ConsentPermission.ConsentPermissionBuilder.class);
    }
    public static AccessPeriod.AccessPeriodBuilder accessPeriod() {
        return easyRandom.nextObject(AccessPeriod.AccessPeriodBuilder.class);
    }
}
