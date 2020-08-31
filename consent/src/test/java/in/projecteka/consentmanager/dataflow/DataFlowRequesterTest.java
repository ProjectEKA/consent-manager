package in.projecteka.consentmanager.dataflow;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.DataFlowRequestClient;
import in.projecteka.consentmanager.dataflow.model.AccessPeriod;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.ConsentStatus;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResult;
import in.projecteka.consentmanager.dataflow.model.DateRange;
import in.projecteka.consentmanager.dataflow.model.HIUReference;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.library.clients.model.ClientError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Objects;

import static in.projecteka.consentmanager.common.TestBuilders.gatewayDataFlowRequest;
import static in.projecteka.consentmanager.dataflow.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.dataflow.TestBuilders.dataFlowRequest;
import static in.projecteka.consentmanager.dataflow.TestBuilders.healthInformationNotificationRequest;
import static in.projecteka.consentmanager.dataflow.TestBuilders.healthInformationResponseBuilder;
import static in.projecteka.consentmanager.dataflow.Utils.toDate;
import static in.projecteka.consentmanager.dataflow.Utils.toDateWithMilliSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class DataFlowRequesterTest {
    @Mock
    private DataFlowRequestRepository dataFlowRequestRepository;

    @Mock
    private ConsentManagerClient consentManagerClient;

    @Mock
    private PostDataFlowRequestApproval postDataFlowRequestApproval;

    @Mock
    private DataFlowRequestClient dataFlowRequestClient;

    @Captor
    private ArgumentCaptor<DataFlowRequestResult> dataFlowRequestResultCaptor;

    private DataFlowRequester dataFlowRequester;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        dataFlowRequester = new DataFlowRequester(consentManagerClient, dataFlowRequestRepository,
                postDataFlowRequestApproval, dataFlowRequestClient);
    }

    @Test
    void shouldAcceptDataFlowRequest() {
        String hiuId = "10000005";
        var request = dataFlowRequest()
                .dateRange(DateRange.builder()
                        .from(toDate("2020-01-15T08:47:48"))
                        .to(toDate("2020-01-20T08:47:48")).build())
                .build();
        var consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.setStatus(ConsentStatus.GRANTED);
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));
        consentArtefactRepresentation.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48"))
                        .toDate(toDate("2020-01-20T08:47:48"))
                        .build());

        when(consentManagerClient.getConsentArtefact(request.getConsent().getId()))
                .thenReturn(Mono.just(consentArtefactRepresentation));
        when(dataFlowRequestRepository.addDataFlowRequest(anyString(),
                any(in.projecteka.consentmanager.dataflow.model.DataFlowRequest.class)))
                .thenReturn(Mono.create(MonoSink::success));
        when(postDataFlowRequestApproval.broadcastDataFlowRequest(anyString(),
                any(in.projecteka.consentmanager.dataflow.model.DataFlowRequest.class))).thenReturn(Mono.empty());

        StepVerifier.create(dataFlowRequester.requestHealthData(request))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
    }

    @Test
    void shouldThrowInvalidHIU() {
        in.projecteka.consentmanager.dataflow.model.DataFlowRequest request = dataFlowRequest().build();
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();

        when(consentManagerClient.getConsentArtefact(request.getConsent().getId()))
                .thenReturn(Mono.just(consentArtefactRepresentation));
        when(postDataFlowRequestApproval.broadcastDataFlowRequest(anyString(),
                any(in.projecteka.consentmanager.dataflow.model.DataFlowRequest.class))).thenReturn(Mono.empty());

        StepVerifier.create(dataFlowRequester.requestHealthData(request))
                .expectErrorMatches(e -> (e instanceof ClientError) && ((ClientError) e).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    void shouldNotifyHealthInfoStatus() {
        var healthInformationNotificationRequest = healthInformationNotificationRequest().build();

        when(dataFlowRequestRepository.getIfPresent(healthInformationNotificationRequest.getRequestId()))
                .thenReturn(Mono.empty());
        when(dataFlowRequestRepository.saveHealthNotificationRequest(healthInformationNotificationRequest))
                .thenReturn(Mono.create(MonoSink::success));

        StepVerifier.create(dataFlowRequester.notifyHealthInformationStatus(healthInformationNotificationRequest))
                .verifyComplete();

        verify(dataFlowRequestRepository,
                times(1)).saveHealthNotificationRequest(healthInformationNotificationRequest);
        verify(dataFlowRequestRepository,
                times(1)).getIfPresent(healthInformationNotificationRequest.getRequestId());
    }

    @Test
    void shouldUpdateHealthInfoStatus() {
        var healthInformationResponse = healthInformationResponseBuilder().build();

        when(dataFlowRequestRepository.updateDataFlowRequestStatus(healthInformationResponse.getHiRequest().getTransactionId(),
                healthInformationResponse.getHiRequest().getSessionStatus())).thenReturn(Mono.create(MonoSink::success));

        StepVerifier.create(dataFlowRequester.updateDataflowRequestStatus(healthInformationResponse))
                .verifyComplete();
    }

    @Test
    void shouldLogErrorWhenAcknowledgementIsAbsent() {
        var gatewayResponse = GatewayResponse.builder().requestId("requestId").build();
        var healthInformationResponse = healthInformationResponseBuilder()
                .resp(gatewayResponse)
                .hiRequest(null)
                .build();

        Logger logger = (Logger) LoggerFactory.getLogger(DataFlowRequester.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        StepVerifier.create(dataFlowRequester.updateDataflowRequestStatus(healthInformationResponse))
                .verifyComplete();
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("DataFlowRequest failed for request id requestId",logsList.get(0).getFormattedMessage());
    }

    @Test
    void shouldCallGatewayWithHiRequest() {
        var hiuId = "10000005";
        var dataFlowRequest = gatewayDataFlowRequest().build();
        dataFlowRequest.getHiRequest().getDateRange().setFrom(toDate("2020-01-15T08:47:48"));
        dataFlowRequest.getHiRequest().getDateRange().setTo(toDate("2020-01-20T08:47:48"));
        var consentArtefact = consentArtefactRepresentation().build();
        consentArtefact.setStatus(ConsentStatus.GRANTED);
        consentArtefact.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        consentArtefact.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));
        consentArtefact.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48"))
                        .toDate(toDate("2020-01-20T08:47:48"))
                        .build());
        when(consentManagerClient.getConsentArtefact(dataFlowRequest.getHiRequest().getConsent().getId()))
                .thenReturn(Mono.just(consentArtefact));
        when(dataFlowRequestRepository.addDataFlowRequest(anyString(),
                any(in.projecteka.consentmanager.dataflow.model.DataFlowRequest.class)))
                .thenReturn(Mono.empty());
        when(dataFlowRequestClient.sendHealthInformationResponseToGateway(dataFlowRequestResultCaptor.capture(), eq(hiuId)))
                .thenReturn(Mono.empty());
        when(postDataFlowRequestApproval.broadcastDataFlowRequest(anyString(),any())).thenReturn(Mono.empty());

        var producer = dataFlowRequester.requestHealthDataInfo(dataFlowRequest);

        StepVerifier.create(producer)
                .verifyComplete();

        verify(postDataFlowRequestApproval,times(1)).broadcastDataFlowRequest(any(),any());
        verify(consentManagerClient,times(1)).getConsentArtefact(eq(dataFlowRequest.getHiRequest().getConsent().getId()));
        verify(dataFlowRequestRepository,times(1)).addDataFlowRequest(any(),any());
        verify(dataFlowRequestClient,times(1)).sendHealthInformationResponseToGateway(any(),anyString());
        assertThat(dataFlowRequestResultCaptor.getValue().getHiRequest()).isNotNull();
        assertThat(dataFlowRequestResultCaptor.getValue().getError()).isNull();
    }

    @Test
    void shouldCallGatewayWhenConsentArtefactNotFound() {
        var hiuId = "";
        var dataFlowRequest = gatewayDataFlowRequest().build();
        when(consentManagerClient.getConsentArtefact(dataFlowRequest.getHiRequest().getConsent().getId()))
                .thenReturn(Mono.error(ClientError.consentArtefactNotFound()));
        when(dataFlowRequestClient.sendHealthInformationResponseToGateway(dataFlowRequestResultCaptor.capture(), eq(hiuId)))
                .thenReturn(Mono.empty());

        var producer = dataFlowRequester.requestHealthDataInfo(dataFlowRequest);

        StepVerifier.create(producer)
                .verifyComplete();
        assertThat(dataFlowRequestResultCaptor.getValue().getHiRequest()).isNull();
        assertThat(dataFlowRequestResultCaptor.getValue().getError()).isNotNull();
    }
}
