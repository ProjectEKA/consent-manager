package in.projecteka.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ConsentArtefactRepresentation {
    private ConsentStatus status;
    private ConsentArtefact consentDetail;
    private String signature;

    public LocalDateTime toDate() {
        return consentDetail.getPermission().getDateRange().getToDate();
    }

    public LocalDateTime fromDate() {
        return consentDetail.getPermission().getDateRange().getFromDate();
    }
}
