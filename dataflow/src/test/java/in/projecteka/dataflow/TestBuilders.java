package in.projecteka.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.dataflow.model.DataFlowRequest;
import in.projecteka.dataflow.model.GatewayDataFlowRequest;
import in.projecteka.dataflow.model.HealthInfoNotificationRequest;
import in.projecteka.dataflow.model.HealthInformationResponse;
import in.projecteka.dataflow.model.hip.DataRequest;
import org.jeasy.random.EasyRandom;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public static LocalDateTime toDate(String date) {
        String pattern = "yyyy-MM-dd['T'HH[:mm][:ss][.SSS][+0000]]";
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime toDateWithMilliSeconds(String dateExpiryAt) {
        return new Timestamp(Long.parseLong(dateExpiryAt)).toLocalDateTime();
    }
}
