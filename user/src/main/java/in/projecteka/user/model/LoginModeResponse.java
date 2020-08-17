package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class LoginModeResponse {
    private final LoginMode loginMode;
}
