package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.TransactionPin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.PrivateKey;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.consentmanager.user.TransactionPinService.blockedKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionPinServiceTest {
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private PrivateKey privateKey;
    @Mock
    private UserServiceProperties userServiceProperties;
    @Mock
    private CacheAdapter<String,String> dayCache;

    private TransactionPinService transactionPinService;
    private TransactionPinRepository transactionPinRepository;

    @BeforeEach
    public void init() {
        transactionPinRepository = Mockito.mock(TransactionPinRepository.class);
        MockitoAnnotations.initMocks(this);
        transactionPinService = Mockito.spy(new TransactionPinService(transactionPinRepository,encoder,privateKey,userServiceProperties,dayCache));
    }

    @Test
    public void shouldValidateRequestId() {
        String patientId = "testPatient";
        String pin = "6666";
        UUID requestId = UUID.randomUUID();
        String scope = "testscope";

        doReturn(Mono.just(false)).when(transactionPinService).validateRequest(requestId);

        Mono<Token> validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, scope);

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError)throwable).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verify();
    }

    @Test
    public void shouldThrowErrorAfterBlocked() {
        String patientId = "testPatient";
        String pin = "6666";
        UUID requestId = UUID.randomUUID();
        String scope = "testscope";
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(Mono.empty());
        doReturn(Mono.just(true)).when(transactionPinService).validateRequest(requestId);
        TransactionPin mockTransactionPin = Mockito.mock(TransactionPin.class);
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(Mono.just(Optional.of(mockTransactionPin)));
        doReturn(Mono.just(true)).when(dayCache).exists(blockedKey(patientId));
        doReturn(new Token("")).when(transactionPinService).newToken(patientId,scope);

        Mono<Token> validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, scope);

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError)throwable).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
    }

    @Test
    public void shouldCountInvalidAttempts() {
        String patientId = "testPatient";
        String pin = "6666";
        UUID requestId = UUID.randomUUID();
        String scope = "testscope";
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(Mono.empty());
        doReturn(Mono.just(true)).when(transactionPinService).validateRequest(requestId);
        TransactionPin mockTransactionPin = Mockito.mock(TransactionPin.class);
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(Mono.just(Optional.of(mockTransactionPin)));
        doReturn(Mono.just(false)).when(dayCache).exists(blockedKey(patientId));
        doReturn(new Token("")).when(transactionPinService).newToken(patientId,scope);
        when(encoder.matches(pin, mockTransactionPin.getPin())).thenReturn(false);
        when(dayCache.increment(TransactionPinService.incorrectAttemptKey(patientId))).thenReturn(Mono.just(1L));
        when(userServiceProperties.getMaxIncorrectPinAttempts()).thenReturn(5L);

        Mono<Token> validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, scope);

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError)throwable).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
        verify(dayCache).increment(TransactionPinService.incorrectAttemptKey(patientId));
    }

    @Test
    public void shouldSetBlockedUponMaximumAttempts() {
        String patientId = "testPatient";
        String pin = "6666";
        UUID requestId = UUID.randomUUID();
        String scope = "testscope";
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(Mono.empty());
        doReturn(Mono.just(true)).when(transactionPinService).validateRequest(requestId);
        TransactionPin mockTransactionPin = Mockito.mock(TransactionPin.class);
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(Mono.just(Optional.of(mockTransactionPin)));
        doReturn(Mono.just(false)).when(dayCache).exists(blockedKey(patientId));
        doReturn(new Token("")).when(transactionPinService).newToken(patientId,scope);
        when(encoder.matches(pin, mockTransactionPin.getPin())).thenReturn(false);
        when(dayCache.increment(TransactionPinService.incorrectAttemptKey(patientId))).thenReturn(Mono.just(5L));
        when(userServiceProperties.getMaxIncorrectPinAttempts()).thenReturn(5L);
        when(dayCache.put(blockedKey(patientId),"true")).thenReturn(Mono.empty());

        Mono<Token> validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, scope);

        StepVerifier.create(validatePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError)throwable).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
        verify(dayCache).increment(TransactionPinService.incorrectAttemptKey(patientId));
        verify(dayCache).put(blockedKey(patientId),"true");
    }

    @Test
    public void shouldGetToken() {
        String patientId = "testPatient";
        String pin = "6666";
        UUID requestId = UUID.randomUUID();
        String scope = "testscope";
        String expectedToken = "testToken";
        when(transactionPinRepository.updateRequestId(eq(requestId), eq(patientId))).thenReturn(Mono.empty());
        doReturn(Mono.just(true)).when(transactionPinService).validateRequest(requestId);
        TransactionPin mockTransactionPin = Mockito.mock(TransactionPin.class);
        when(transactionPinRepository.getTransactionPinByPatient(patientId)).thenReturn(Mono.just(Optional.of(mockTransactionPin)));
        doReturn(Mono.just(false)).when(dayCache).exists(blockedKey(patientId));
        doReturn(new Token(expectedToken)).when(transactionPinService).newToken(patientId,scope);
        when(encoder.matches(pin, mockTransactionPin.getPin())).thenReturn(true);
//        when(dayCache.increment(TransactionPinService.incorrectAttemptKey(patientId))).thenReturn(Mono.just(5L));
        when(userServiceProperties.getMaxIncorrectPinAttempts()).thenReturn(5L);
//        when(dayCache.put(blockedKey(patientId),"true")).thenReturn(Mono.empty());

        Mono<Token> validatePinMono = transactionPinService.validatePinFor(patientId, pin, requestId, scope);

        StepVerifier.create(validatePinMono)
                .assertNext(token -> assertThat(token.getTemporaryToken()).isEqualTo(expectedToken))
                .verifyComplete();
    }

    @Test
    void shouldThrowErrorForChangingInvalidTransactionPin() {
        String patientId = "testPatient";
        String pin = "666";

        when(userServiceProperties.getTransactionPinDigitSize()).thenReturn(4);
        Mono<Void> changePinMono = transactionPinService.changeTransactionPinFor(patientId, pin);

        StepVerifier.create(changePinMono)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError)throwable).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verify();
    }

    @Test
    void shouldReturnAcceptedForChangingValidTransactionPin() {
        String patientId = "testPatient";
        String pin = "6666";
        String encodedPin = encoder.encode(pin);

        when(userServiceProperties.getTransactionPinDigitSize()).thenReturn(4);
        when(transactionPinRepository.changeTransactionPin(eq(patientId), eq(encodedPin))).thenReturn(Mono.empty());
        Mono<Void> changePinMono = transactionPinService.changeTransactionPinFor(patientId, pin);

        StepVerifier.create(changePinMono).verifyComplete();
    }
}