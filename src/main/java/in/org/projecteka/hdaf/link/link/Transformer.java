package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.link.model.hip.CareContext;
import in.org.projecteka.hdaf.link.link.model.hip.Patient;

import java.util.List;
import java.util.stream.Collectors;

public class Transformer {
    private static List<CareContext> toHIPCareContext(List<in.org.projecteka.hdaf.link.link.model.CareContext> careContexts) {
        return careContexts.stream().map(careContext -> new CareContext(careContext.getReferenceNumber())).collect(Collectors.toList());
    }

    public static Patient toHIPPatient(String patientId, in.org.projecteka.hdaf.link.link.model.Patient patient) {
        List<in.org.projecteka.hdaf.link.link.model.CareContext> careContexts = patient.getCareContexts();
        List<CareContext> careContextsInHIP = toHIPCareContext(careContexts);
        return new Patient(patientId, patient.getReferenceNumber(), careContextsInHIP);
    }
}
