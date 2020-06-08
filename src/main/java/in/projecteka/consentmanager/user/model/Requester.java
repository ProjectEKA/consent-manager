package in.projecteka.consentmanager.user.model;

import lombok.Getter;

@Getter
public enum Requester {
    HIU("HIU"),
    HIP("HIP");

    private final String value;

    Requester(String val){
        value = val;
    }
}
