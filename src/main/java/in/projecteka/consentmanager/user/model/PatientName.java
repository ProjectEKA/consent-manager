package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class PatientName {
    private final String firstName;
    private final String middleName;
    private final String lastName;

    public String getFullName(){
        var fullName = firstName;

        if (middleName != null){
            fullName += " " + middleName;
        }

        if (lastName != null){
            fullName += " " + lastName;
        }
        return fullName;
    }
}
