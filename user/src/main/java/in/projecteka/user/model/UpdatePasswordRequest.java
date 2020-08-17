package in.projecteka.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class UpdatePasswordRequest {
    private final String oldPassword;
    private final String newPassword;
}
