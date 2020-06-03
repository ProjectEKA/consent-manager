package in.projecteka.consentmanager.dataflow.model;

import lombok.*;

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
