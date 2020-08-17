package in.projecteka.consentmanager.properties;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
class KeyPairConfigTest {

    @Autowired
    private KeyPairConfig keyPairConfig;

    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;
    
    @Test
    void shouldCreateSignArtefactKeyPair() {
        KeyPair signArtefactKeyPair = keyPairConfig.createSignArtefactKeyPair();
        assertThat(signArtefactKeyPair).isNotNull();
        assertThat(signArtefactKeyPair.getPublic()).isNotNull();
        assertThat(signArtefactKeyPair.getPrivate()).isNotNull();
    }

    @Test
    void shouldCreatePinVerificationKeyPair() {
        KeyPair pinVerificationKeyPair = keyPairConfig.createPinVerificationKeyPair();
        assertThat(pinVerificationKeyPair).isNotNull();
        assertThat(pinVerificationKeyPair.getPublic()).isNotNull();
        assertThat(pinVerificationKeyPair.getPrivate()).isNotNull();
    }
}