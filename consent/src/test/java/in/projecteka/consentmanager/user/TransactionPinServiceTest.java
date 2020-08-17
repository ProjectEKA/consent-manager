package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.TransactionPin;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.UUID;

import static in.projecteka.consentmanager.common.TestBuilders.string;
import static in.projecteka.consentmanager.common.TestBuilders.transactionPin;
import static in.projecteka.consentmanager.user.TransactionPinService.blockedKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

class TransactionPinServiceTest {
    @Mock
    private BCryptPasswordEncoder encoder;

    private final int transactionPinDigitSize;
    private final long maxIncorrectAttempts;
    private final PrivateKey privateKey;

    @Mock
    private CacheAdapter<String, String> dayCache;

    @Mock
    private TransactionPinRepository transactionPinRepository;

    private TransactionPinService transactionPinService;

    public TransactionPinServiceTest() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var kp = kpg.generateKeyPair();
        this.privateKey = kp.getPrivate();
        this.transactionPinDigitSize = 4;
        this.maxIncorrectAttempts = 5;
    }

    @BeforeEach
    void init() {
        initMocks(this);
        transactionPinService = new TransactionPinService(transactionPinRepository,
                encoder,
                privateKey,
                UserServiceProperties.builder()
                        .transactionPinDigitSize(transactionPinDigitSize)
                        .maxIncorrectPinAttempts(maxIncorrectAttempts)
                        .build(),
                dayCache);
    }

    @Test
    void shouldValidateRequestId() {
        var patientId = string();
        var pin = string();
        var requestId = UUID.randomUUID();
        when(transactionPinRepository.getTransactionPinByRequest(requestId))
                .thenReturn(just(new TransactionPin(pin, patientId)));

        Mono<Token> validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, string());

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError) throwable).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verify();
    }

    @Test
    void shouldThrowErrorAfterBlocked() {
        var patientId = string();
        var pin = string();
        var requestId = UUID.randomUUID();
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(empty());
        when(transactionPinRepository.getTransactionPinByRequest(requestId)).thenReturn(empty());
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(just(transactionPin().build()));
        doReturn(just(true)).when(dayCache).exists(blockedKey(patientId));

        Mono<Token> validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, string());

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError) throwable).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
    }

    @Test
    void shouldCountInvalidAttempts() {
        var patientId = string();
        var pin = string();
        var requestId = UUID.randomUUID();
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(empty());
        when(transactionPinRepository.getTransactionPinByRequest(requestId)).thenReturn(empty());
        var transactionPin = transactionPin().build();
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(just(transactionPin));
        doReturn(just(false)).when(dayCache).exists(blockedKey(patientId));
        when(encoder.matches(pin, transactionPin.getPin())).thenReturn(false);
        when(dayCache.increment(TransactionPinService.incorrectAttemptKey(patientId))).thenReturn(just(1L));

        var validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, string());

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError) throwable).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
        verify(dayCache).increment(TransactionPinService.incorrectAttemptKey(patientId));
    }

    @Test
    void shouldSetBlockedUponMaximumAttempts() {
        var patientId = string();
        var pin = string();
        var requestId = UUID.randomUUID();
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(empty());
        when(transactionPinRepository.getTransactionPinByRequest(requestId)).thenReturn(empty());
        var transactionPin = transactionPin().build();
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(just(transactionPin));
        doReturn(just(false)).when(dayCache).exists(blockedKey(patientId));
        when(encoder.matches(pin, transactionPin.getPin())).thenReturn(false);
        when(dayCache.increment(TransactionPinService.incorrectAttemptKey(patientId)))
                .thenReturn(just(maxIncorrectAttempts));
        when(dayCache.put(blockedKey(patientId), "true")).thenReturn(empty());

        var validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, string());

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError) throwable).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
        verify(dayCache).increment(TransactionPinService.incorrectAttemptKey(patientId));
        verify(dayCache).put(blockedKey(patientId), "true");
    }

    @Test
    void shouldGetToken() {
        var patientId = string();
        var pin = string();
        var requestId = UUID.randomUUID();
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(empty());
        when(transactionPinRepository.getTransactionPinByRequest(requestId)).thenReturn(empty());
        var transactionPin = transactionPin().build();
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(just(transactionPin));
        doReturn(just(false)).when(dayCache).exists(blockedKey(patientId));
        when(encoder.matches(pin, transactionPin.getPin())).thenReturn(true);

        var validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, string());

        StepVerifier.create(validatePinMono)
                .assertNext(token -> assertThat(token.getTemporaryToken()).isNotEmpty())
                .verifyComplete();
    }

    @Test
    void shouldThrowErrorForChangingInvalidTransactionPin() {
        var changePinMono = transactionPinService.changeTransactionPinFor(string(), string());

        StepVerifier.create(changePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError) throwable).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verify();
    }

    @Test
    void shouldReturnAcceptedForChangingValidTransactionPin() {
        var patientId = string();
        var pin = string(transactionPinDigitSize);
        var encodedPin = encoder.encode(pin);
        when(transactionPinRepository.changeTransactionPin(eq(patientId), eq(encodedPin))).thenReturn(empty());

        var changePinMono = transactionPinService.changeTransactionPinFor(patientId, pin);

        StepVerifier.create(changePinMono).verifyComplete();
    }
}