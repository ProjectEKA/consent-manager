package in.projecteka.user;

import in.projecteka.user.model.Identifier;
import in.projecteka.user.model.IdentifierType;
import in.projecteka.user.model.Profile;
import io.vertx.core.json.JsonArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static in.projecteka.user.TestBuilders.string;
import static in.projecteka.user.TestBuilders.user;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProfileServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private TransactionPinService transactionPinService;

    private ProfileService profileService;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        profileService = new ProfileService(userService, transactionPinService);
    }

    @Test
    public void shouldReturnPatientProfile() {
        var patientId = string();
        var user = user().build();
        var profile = Profile.builder()
                .id(user.getIdentifier())
                .name(user.getName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .hasTransactionPin(true)
                .verifiedIdentifiers(singletonList(new Identifier(IdentifierType.MOBILE, user.getPhone())));
        JsonArray unverifiedIdentifiersJson = user.getUnverifiedIdentifiers();
        if (unverifiedIdentifiersJson !=null) {
            List<Identifier> unverifiedIdentifiers = IntStream.range(0, user.getUnverifiedIdentifiers().size())
                    .mapToObj(unverifiedIdentifiersJson::getJsonObject)
                    .map(jsonObject -> new Identifier(IdentifierType.valueOf(jsonObject.getString("type")),jsonObject.getString("value")))
                    .collect(Collectors.toList());
            Profile.builder().unverifiedIdentifiers(unverifiedIdentifiers).build();
        }

        Mockito.when(userService.userWith(patientId)).thenReturn(Mono.just(user));
        Mockito.when(transactionPinService.isTransactionPinSet(patientId)).thenReturn(Mono.just(true));

        StepVerifier.create(profileService.profileFor(patientId))
                .assertNext(response -> assertThat(response.getId()).isEqualTo(profile.build().getId()))
                .verifyComplete();
    }
}

