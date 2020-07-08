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

    public String createFullName(){
        var fullName = first;

        if (middle != null && !middle.isEmpty()){
            fullName += " " + middle;
        }

        if (last != null && !last.isEmpty()){
            fullName += " " + last;
        }
        return fullName;
    }
}
