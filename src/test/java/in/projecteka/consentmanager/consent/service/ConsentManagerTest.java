package in.projecteka.consentmanager.consent.service;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.model.request.ConsentDetail;
import in.projecteka.consentmanager.consent.model.request.HIPReference;
import in.projecteka.consentmanager.consent.model.request.HIUReference;
import in.projecteka.consentmanager.consent.model.request.PatientReference;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ConsentManagerTest {

    @Mock
    ConsentRequestRepository repository;
    @Mock
    ClientRegistryClient providerClient;

    @Mock
    UserServiceClient userClient;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void askForConsent() {
        HIPReference hip1 = HIPReference.builder().id("hip1").build();
        HIUReference hiu1 = HIUReference.builder().id("hiu1").build();
        PatientReference patient = PatientReference.builder().id("chethan@ncg").build();
        ConsentDetail consentDetail = ConsentDetail.builder().hip(hip1).hiu(hiu1).patient(patient).build();

        ConsentManager consentManager = new ConsentManager(repository, providerClient, userClient);
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when (providerClient.providerWith(eq("hip1"))).thenReturn(Mono.just(new Provider()));
        when (providerClient.providerWith(eq("hiu1"))).thenReturn(Mono.just(new Provider()));
        when (userClient.userOf(eq("chethan@ncg"))).thenReturn(Mono.just(new User()));

        String requestingHIUId = "hiu1";
        StepVerifier.create(consentManager.askForConsent(requestingHIUId, consentDetail)).expectNextMatches(r -> r != null).verifyComplete();

    }
}