package in.projecteka.consentmanager.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Slf4j
@Getter
@AllArgsConstructor
@ConstructorBinding
@ConfigurationProperties(prefix = "keystore")
public class KeyPairConfig {
    @Value("${keystore.file-path}")
    private final String keyStoreFilePath;

    @Value("${keystore.password}")
    private final String keyStorePassword;

    @Value("${keystore.sign-artefact-keypair.storetype}")
    private final String signArtefactKeyPairType;

    @Value("${keystore.sign-artefact-keypair.alias}")
    private final String signArtefactKeyPairAlias;

    @Value("${keystore.pin-verification-keypair.storetype}")
    private final String pinVerificationKeyPairType;

    @Value("${keystore.pin-verification-keypair.alias}")
    private final String pinVerificationKeyPairAlias;

    public KeyPair createSignArtefactKeyPair() {
        return getKeyPairForAlias(signArtefactKeyPairAlias, signArtefactKeyPairType);
    }

    public KeyPair createPinVerificationKeyPair() {
        return getKeyPairForAlias(pinVerificationKeyPairAlias, pinVerificationKeyPairType);
    }

    @SneakyThrows
    private KeyPair getKeyPairForAlias(String keyPairAlias, String keyPairType) {
        final KeyStore keyStore = KeyStore.getInstance(keyPairType);
        File file = new File(keyStoreFilePath);
        char[] pwdArray = keyStorePassword.toCharArray();
        keyStore.load(new FileInputStream(file), pwdArray);
        Certificate certificate = keyStore.getCertificate(keyPairAlias);
        if (certificate == null) {
            log.error("No certificate found for given keystore 'alias'");
            throw new KeyStoreException("No certificate found for given keystore 'alias'");
        }
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyPairAlias, pwdArray);
        PublicKey publicKey = certificate.getPublicKey();
        return new KeyPair(publicKey, privateKey);
    }
}
