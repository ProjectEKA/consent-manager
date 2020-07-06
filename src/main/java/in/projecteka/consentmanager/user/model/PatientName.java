package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class PatientName {
    private final String fName;
    private final String mName;
    private final String lName;

    public String getFullName(){
        var fullName = fName;

        if (mName != null){
            fullName += " " + mName;
        }

        if (lName != null){
            fullName += " " + lName;
        }
        return fullName;
    }
}
