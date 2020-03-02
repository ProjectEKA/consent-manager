package in.projecteka.consentmanager.dataflow;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.dataflow.model.AccessPeriod;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.HIDataRange;
import in.projecteka.consentmanager.dataflow.model.HIUReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.test.StepVerifier;

import java.text.ParseException;

import static in.projecteka.consentmanager.dataflow.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.dataflow.TestBuilders.dataFlowRequest;
import static in.projecteka.consentmanager.dataflow.Utils.toDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DataFlowRequesterTest {
    @Mock
    private DataFlowRequestRepository dataFlowRequestRepository;

    @Mock
    private ConsentManagerClient consentManagerClient;

    @Mock
    private PostDataFlowRequestApproval postDataFlowRequestApproval;

    private DataFlowRequester dataFlowRequester;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        dataFlowRequester = new DataFlowRequester(consentManagerClient, dataFlowRequestRepository,
                postDataFlowRequestApproval);
    }

    @Test
    public void shouldAcceptDataFlowRequest() throws ParseException {
        String hiuId = "10000005";
        in.projecteka.consentmanager.dataflow.model.DataFlowRequest request = dataFlowRequest().build();
        request.setHiDataRange(HIDataRange.builder().from(toDate("2020-01-16T08:47:48Z")).to(toDate("2020" +
                "-01-20T08:47:48Z")).build());
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        consentArtefactRepresentation.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48Z"))
                        .toDate(toDate("2020-01-29T08:47:48Z"))
                        .build());

        when(consentManagerClient.getConsentArtefact(request.getConsent().getId()))
                .thenReturn(Mono.just(consentArtefactRepresentation));
        when(dataFlowRequestRepository.addDataFlowRequest(anyString(),
                any(in.projecteka.consentmanager.dataflow.model.DataFlowRequest.class)))
                .thenReturn(Mono.create(MonoSink::success));
        when(postDataFlowRequestApproval.broadcastDataFlowRequest(anyString(),
                any(in.projecteka.consentmanager.dataflow.model.DataFlowRequest.class))).thenReturn(Mono.empty());

        StepVerifier.create(dataFlowRequester.requestHealthData(hiuId, request))
                .expectNextMatches(res -> res != null)
                .verifyComplete();
    }

    @Test
    public void shouldThrowInvalidHIU() {
        in.projecteka.consentmanager.dataflow.model.DataFlowRequest request = dataFlowRequest().build();
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();

        when(consentManagerClient.getConsentArtefact(request.getConsent().getId()))
                .thenReturn(Mono.just(consentArtefactRepresentation));

        StepVerifier.create(dataFlowRequester.requestHealthData("1", request))
                .expectErrorMatches(e -> (e instanceof ClientError) && ((ClientError) e).getHttpStatus().is4xxClientError());
    }
}
