package in.projecteka.consentmanager.common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static in.projecteka.consentmanager.common.Role.GATEWAY;
import static in.projecteka.consentmanager.common.TestBuilders.string;
import static java.lang.String.format;
import static reactor.test.StepVerifier.create;

class CentralRegistryTokenVerifierTest {

    @Test
    void returnCallerWithGatewayRoleWhenTokenHasRoles() throws JOSEException {
        var clientId = string();
        JSONArray roleValues = new JSONArray();
        var roles = List.of(GATEWAY);
        var randomRole1 = string();
        var randomRole2 = string();
        roleValues.add(randomRole1);
        roleValues.add(randomRole2);
        roleValues.addAll(roles);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roleValues);
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .claim("realm_access", realmAccess)
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        ServiceCaller caller = new ServiceCaller(clientId, roles);

        var centralRegistryTokenVerifierForGateway = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<ServiceCaller> verify = centralRegistryTokenVerifierForGateway.verify(format("bearer %s", token));

        create(verify).expectNext(caller).verifyComplete();
    }

    @Test
    void returnCallerWithEmptyWhenTokenDoesNotHaveValidRoles() throws JOSEException {
        var clientId = string();
        JSONArray roleValues = new JSONArray();
        var randomRole1 = string();
        var randomRole2 = string();
        roleValues.add(randomRole1);
        roleValues.add(randomRole2);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roleValues);
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .claim("realm_access", realmAccess)
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        var centralRegistryTokenVerifierForGateway = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<ServiceCaller> verify = centralRegistryTokenVerifierForGateway.verify(format("bearer %s", token));

        create(verify).expectNext(new ServiceCaller(clientId,  List.of())).verifyComplete();
    }


    @Test
    void returnEmptyWhenTokenDoesNotHaveResourceAccess() throws JOSEException {
        var clientId = string();
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        var centralRegistryTokenVerifierForGateway = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<ServiceCaller> verify = centralRegistryTokenVerifierForGateway.verify(format("bearer %s", token));

        create(verify).verifyComplete();
    }

    @Test
    void returnEmptyWhenTokenDoesNotHaveProperResourceAccess() throws JOSEException {
        var clientId = string();
        var realmAccess = string();
        var rsaKey = new RSAKeyGenerator(2048).keyID(string()).generate();
        var signer = new RSASSASigner(rsaKey);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("scope", string())
                .issueTime(new Date())
                .claim("clientId", clientId)
                .claim("realm_access", realmAccess)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();
        var centralRegistryTokenVerifierForGateway = new CentralRegistryTokenVerifier(new JWKSet(rsaKey.toPublicJWK()));

        Mono<ServiceCaller> verify = centralRegistryTokenVerifierForGateway.verify(format("bearer %s", token));

        create(verify).verifyComplete();
    }
}