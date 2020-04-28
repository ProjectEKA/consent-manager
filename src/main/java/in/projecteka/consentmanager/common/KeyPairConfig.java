package in.projecteka.consentmanager.common;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Slf4j
@Component
@NoArgsConstructor
public class KeyPairConfig {
    @Value("${keyStore.filePath}")
    private String keyStoreFilePath;

    @Value("${keyStore.password}")
    private String keyStorePassword;

    @Value("${keyStore.signArtefactKeyPair.storeType}")
    private String signArtefactKeyPairType;

    @Value("${keyStore.signArtefactKeyPair.alias}")
    private String signArtefactKeyPairAlias;

    @Value("${keyStore.pinVerificationKeyPair.storeType}")
    private String pinVerificationKeyPairType;

    @Value("${keyStore.pinVerificationKeyPair.alias}")
    private String pinVerificationKeyPairAlias;


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
