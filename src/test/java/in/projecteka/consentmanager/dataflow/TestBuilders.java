package in.projecteka.consentmanager.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestMessage;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final EasyRandom easyRandom = new EasyRandom();

    public static DataFlowRequest.DataFlowRequestBuilder dataFlowRequest() {
        return easyRandom.nextObject(DataFlowRequest.DataFlowRequestBuilder.class);
    }

    public static ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder consentArtefactRepresentation() {
        return easyRandom.nextObject(ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder.class);
    }

    public static DataFlowRequestMessage.DataFlowRequestMessageBuilder dataFlowRequestMessage() {
        return easyRandom.nextObject(DataFlowRequestMessage.DataFlowRequestMessageBuilder.class);
    }

    public static in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest.DataFlowRequestBuilder dataFlowRequestBuilder() {
        return easyRandom.nextObject(in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest.DataFlowRequestBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static Provider.ProviderBuilder provider() {
        return easyRandom.nextObject(Provider.ProviderBuilder.class);
    }
}
