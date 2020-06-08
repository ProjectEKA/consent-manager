package in.projecteka.consentmanager.user.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Patient {
    String id;
    String name;
}