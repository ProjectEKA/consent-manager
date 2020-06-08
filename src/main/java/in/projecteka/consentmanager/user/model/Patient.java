package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Patient {
    private String id;
    private String name;
}