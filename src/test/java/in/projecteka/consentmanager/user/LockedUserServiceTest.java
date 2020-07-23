package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.LockedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LockedUserServiceTest {

    @Mock
    LockedUsersRepository lockedUsersRepository;

    @Mock
    LockedServiceProperties lockedServiceProperties;

    private String patientId;
    private LockedUserService lockedUserService;
    private LocalDateTime currentTime;

    @BeforeEach
    void init() {
        initMocks(this);
        patientId = "someone@ncg";
        currentTime = LocalDateTime.now(ZoneOffset.UTC);
        when(lockedServiceProperties.getCoolOfPeriod()).thenReturn(2);
        when(lockedServiceProperties.getMaximumInvalidAttempts()).thenReturn(5);
        lockedUserService = new LockedUserService(lockedUsersRepository, lockedServiceProperties);
    }

    @Test
    void shouldRemoveLockedUser() {
        when(lockedUsersRepository.deleteUser(eq(patientId))).thenReturn(Mono.empty());
        StepVerifier.create(lockedUserService.removeLockedUser(patientId))
                .verifyComplete();

        verify(lockedUsersRepository, times(1)).deleteUser(patientId);
    }

    @Test
    void shouldUpdateLockedUserWithoutRemovingOldAttemptsWhenFirstAttemptWasInTimeLimit() {
        var lockedUser = new LockedUser(4, patientId, false, currentTime, currentTime.minusMinutes(1));
        when(lockedUsersRepository.getLockedUserFor(eq(patientId))).thenReturn(Mono.just(lockedUser));
        when(lockedUsersRepository.upsert(eq(patientId))).thenReturn(Mono.empty());
        StepVerifier.create(lockedUserService.createOrUpdateLockedUser(patientId))
                .assertNext(remainingTries -> assertEquals(Integer.valueOf(0), remainingTries))
                .verifyComplete();

        verify(lockedUsersRepository, times(0)).deleteUser(patientId);
        verify(lockedUsersRepository, times(1)).upsert(patientId);
    }

    @Test
    void shouldUpdateLockedUserWithRemovingOldAttemptsWhenFirstAttemptWasOutOfTimeLimit() {
        var lockedUser = new LockedUser(5, patientId, false, currentTime, currentTime.minusMinutes(3));
        when(lockedUsersRepository.getLockedUserFor(eq(patientId))).thenReturn(Mono.just(lockedUser));
        when(lockedUsersRepository.upsert(eq(patientId))).thenReturn(Mono.empty());
        when(lockedUsersRepository.deleteUser(eq(patientId))).thenReturn(Mono.empty());
        StepVerifier.create(lockedUserService.createOrUpdateLockedUser(patientId))
                .assertNext(remainingTries -> assertEquals(Integer.valueOf(4), remainingTries))
                .verifyComplete();

        verify(lockedUsersRepository, times(1)).deleteUser(patientId);
        verify(lockedUsersRepository, times(1)).upsert(patientId);
    }
}
