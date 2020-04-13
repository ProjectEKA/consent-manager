package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class SignUpRequest {
    private String name;
    private String userName;
    private String password;
    private Gender gender;
    private Integer yearOfBirth;
}