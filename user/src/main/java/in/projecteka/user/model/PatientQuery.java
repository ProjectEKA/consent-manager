package in.projecteka.user.model;

import in.projecteka.library.common.PatientReference;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PatientQuery {
    PatientReference patient;
    RequesterDetail requester;
}
