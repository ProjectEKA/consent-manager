package in.org.projecteka.hdaf.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@AllArgsConstructor
@Getter
public class User {
    private String userId;
    private String firstName;
    private String lastName;
    private Date dateOfBirth;
}
