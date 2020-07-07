package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class PatientName {
    private final String first;
    private final String middle;
    private final String last;

    public String getFullName(){
        var fullName = first;

        if (middle != null){
            fullName += " " + middle;
        }

        if (last != null){
            fullName += " " + last;
        }
        return fullName;
    }
}
