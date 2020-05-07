package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.LockedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LockedUserServiceTest {

    @Mock
    LockedUsersRepository lockedUsersRepository;


    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    void getLockedUser() {
        var patientId = "patientId";
        var lockedUser = new LockedUser(1, patientId, false, "");
        when(lockedUsersRepository.getLockedUserFor(patientId))
                .thenReturn(Mono.just(lockedUser));
        LockedUserService lockedUserService = new LockedUserService(lockedUsersRepository);
        Mono<LockedUser> sessionPublisher = lockedUserService.userFor(patientId);

        StepVerifier.create(sessionPublisher)
                .assertNext(session -> assertThat(session).isEqualTo(lockedUser))
                .verifyComplete();
    }
}