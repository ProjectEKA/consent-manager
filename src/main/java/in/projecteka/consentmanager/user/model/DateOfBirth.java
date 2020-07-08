package in.projecteka.consentmanager.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class DateOfBirth {
    private final Integer date;
    private final Integer month;
    private final Integer year;
}
