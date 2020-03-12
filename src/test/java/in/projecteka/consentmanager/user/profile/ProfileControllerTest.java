package in.projecteka.consentmanager.user.profile;

import org.junit.jupiter.api.Test;

public class ProfileControllerTest {
    @Test
    public void shouldGetMyProfile() {
        ProfileController profileController = new ProfileController();
        profileController.me();
    }
}
