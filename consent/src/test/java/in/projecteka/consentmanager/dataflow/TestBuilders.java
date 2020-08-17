package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.GatewayDataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInfoNotificationRequest;
import in.projecteka.consentmanager.dataflow.model.HealthInformationResponse;
import in.projecteka.consentmanager.dataflow.model.hip.DataRequest;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static DataFlowRequest.DataFlowRequestBuilder dataFlowRequest() {
        return easyRandom.nextObject(DataFlowRequest.DataFlowRequestBuilder.class);
    }

    public static ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder consentArtefactRepresentation() {
        return easyRandom.nextObject(ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static GatewayDataFlowRequest.GatewayDataFlowRequestBuilder gatewayDataFlowRequest() {
        return easyRandom.nextObject(GatewayDataFlowRequest.GatewayDataFlowRequestBuilder.class);
    }

    public static HealthInfoNotificationRequest.HealthInfoNotificationRequestBuilder healthInformationNotificationRequest() {
        return easyRandom.nextObject(HealthInfoNotificationRequest.HealthInfoNotificationRequestBuilder.class);
    }

    public static HealthInformationResponse.HealthInformationResponseBuilder healthInformationResponseBuilder() {
        return easyRandom.nextObject(HealthInformationResponse.HealthInformationResponseBuilder.class);
    }

    public static DataRequest.DataRequestBuilder dataRequest() {
        return easyRandom.nextObject(DataRequest.DataRequestBuilder.class);
    }
}
