package in.projecteka.consentmanager.common;

import com.nimbusds.jose.jwk.JWKSet;
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

//    @Test
//    void shouldThrowKeyStoreExceptionIfCertificateIsNull() throws KeyStoreException {
//        when(KeyStore.getInstance("PKCS12")).thenReturn(keyStore);
//        when(keyStore.getCertificate("test-sign-artefact")).thenReturn(null);
//        assertThrows(KeyStoreException.class, () -> keyPairConfig.createSignArtefactKeyPair());
//    }
}