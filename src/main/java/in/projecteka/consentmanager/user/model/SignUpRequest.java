package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.validation.Valid;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class SignUpRequest {
    private String name;
    private String username;
    private String password;
    private Gender gender;
    private Integer yearOfBirth;
    @Valid
    private List<Identifier> unverifiedIdentifiers;
}