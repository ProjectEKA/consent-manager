package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.link.HIPClient;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinkRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkResponse;
import in.projecteka.consentmanager.link.link.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static in.projecteka.consentmanager.link.link.TestBuilders.address;
import static in.projecteka.consentmanager.link.link.TestBuilders.identifier;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceResponse;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.provider;
import static in.projecteka.consentmanager.link.link.TestBuilders.telecom;
import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LinkTest {

  @Mock
  private ClientRegistryClient clientRegistryClient;

  @Mock
  private HIPClient hipClient;

  @Mock
  private LinkRepository linkRepository;

  @Mock
  private UserServiceClient userServiceClient;

  @BeforeEach
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void createsLinkReference() {
      var link = new Link(hipClient, clientRegistryClient, linkRepository, userServiceClient);
      var address = address().use("work").build();
      var telecommunication = telecom().use("work").build();
      String providerUrl = "http://localhost:8001";
      var identifier = identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
      var provider = provider()
              .addresses(of(address))
              .telecoms(of(telecommunication))
              .identifiers(of(identifier))
              .name("Max")
              .build();
      PatientLinkReferenceResponse patientLinkReferenceResponse = patientLinkReferenceResponse().build();
      String patientId = "patient";
      PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
      var patientLinkReferenceRequestForHIP = new in.projecteka.consentmanager.link.link.model.hip.PatientLinkReferenceRequest(
              patientLinkReferenceRequest.getTransactionId(),
              toHIPPatient(patientId, patientLinkReferenceRequest.getPatient()));
      String hipId = "10000005";
      patientLinkReferenceResponse.setTransactionId(patientLinkReferenceRequest.getTransactionId());

      when(hipClient.linkPatientCareContext(patientLinkReferenceRequestForHIP, providerUrl))
              .thenReturn(Mono.just(patientLinkReferenceResponse));
      when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
      when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
              .thenReturn(Mono.just(hipId));
      when(linkRepository.insertToLinkReference(patientLinkReferenceResponse, hipId)).thenReturn(Mono.empty());

      StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
              .expectNext(patientLinkReferenceResponse)
              .verifyComplete();
  }

  @Test
  public void shouldGetSystemUrlForOfficialIdentifier() {
      var link = new Link(hipClient, clientRegistryClient, linkRepository, userServiceClient);
      var address = address().use("work").build();
      var telecommunication = telecom().use("work").build();
      String providerUrl = "http://localhost:8001";
      var identifier1 =
              identifier().use("personal").system("personalUrl").build();
      var identifier2 =
              identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
      var provider =
              provider()
                      .addresses(of(address))
                      .telecoms(of(telecommunication))
                      .identifiers(of(identifier1, identifier2))
                      .name("Max")
                      .build();

      String hipId = "10000005";
      when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
      PatientLinkReferenceResponse patientLinkReferenceResponse =
              PatientLinkReferenceResponse.builder().build();
      String patientId = "patient";
      PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
      var patientLinkReferenceRequestForHIP =
              new in.projecteka.consentmanager.link.link.model.hip.PatientLinkReferenceRequest(
                      patientLinkReferenceRequest.getTransactionId(),
                      toHIPPatient(patientId, patientLinkReferenceRequest.getPatient()));

      when(hipClient.linkPatientCareContext(patientLinkReferenceRequestForHIP, providerUrl))
              .thenReturn(Mono.just(patientLinkReferenceResponse));
      when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
              .thenReturn(Mono.just(hipId));
      when(linkRepository.insertToLinkReference(patientLinkReferenceResponse, hipId)).thenReturn(Mono.empty());

      StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
              .expectNext(patientLinkReferenceResponse)
              .verifyComplete();
      verify(hipClient).linkPatientCareContext(patientLinkReferenceRequestForHIP, providerUrl);
  }

  @Test
  public void shouldGetErrorWhenProviderUrlIsEmpty() {
      var link = new Link(hipClient, clientRegistryClient, linkRepository, userServiceClient);
      var address = address().use("work").build();
      var telecommunication = telecom().use("work").build();
      var provider =
              provider()
                      .addresses(of(address))
                      .telecoms(of(telecommunication))
                      .identifiers(asList())
                      .name("Max")
                      .build();
      String patientId = "patient";
      PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
      ClientError clientError = ClientError.unableToConnectToProvider();

      String hipId = "10000005";
      when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
      when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
              .thenReturn(Mono.just(hipId));

      StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
              .expectErrorSatisfies(error -> {
                  assertThat(((ClientError)error).getError()).isEqualTo(clientError.getError());
                  assertThat(((ClientError)error).getHttpStatus()).isEqualTo(clientError.getHttpStatus());
              })
              .verify();
  }

  @Test
  public void linksPatientsCareContexts() {
      var link = new Link(hipClient, clientRegistryClient, linkRepository, userServiceClient);
      var address = address().use("work").build();
      var telecommunication = telecom().use("work").build();
      String providerUrl = "http://localhost:8001";
      var identifier =
              identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
      var provider =
              provider()
                      .addresses(of(address))
                      .telecoms(of(telecommunication))
                      .identifiers(of(identifier))
                      .name("Max")
                      .build();
      PatientLinkResponse patientLinkResponse =
              PatientLinkResponse.builder().build();
      PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
      String linkRefNumber = "link-ref-num";
      String patientId = "patient";

      when(hipClient.validateToken(linkRefNumber, patientLinkRequest, providerUrl))
              .thenReturn(Mono.just(patientLinkResponse));
      String hipId = "10000005";
      when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
      String transactionId = "transactionId";
      ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).withNano(0).plusHours(1);
      when(linkRepository.getExpiryFromLinkReference(linkRefNumber)).thenReturn(Mono.just(zonedDateTime.toString()));
      when(linkRepository.getTransactionIdFromLinkReference(linkRefNumber)).thenReturn(Mono.just(transactionId));
      when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));
      when(linkRepository.insertToLink(hipId, patientId, linkRefNumber, patientLinkResponse.getPatient())).thenReturn(Mono.empty());

      StepVerifier.create(link.verifyToken(linkRefNumber, patientLinkRequest, patientId))
              .expectNext(patientLinkResponse)
              .verifyComplete();
  }
}
