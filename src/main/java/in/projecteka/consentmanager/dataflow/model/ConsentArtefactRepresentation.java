package in.projecteka.consentmanager.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ConsentArtefactRepresentation {
    private ConsentStatus status;
    private ConsentArtefact consentDetail;
    private String signature;

    public Date toDate() {
        return consentDetail.getPermission().getDateRange().getToDate();
    }

    public Date fromDate() {
        return consentDetail.getPermission().getDateRange().getFromDate();
    }
}
