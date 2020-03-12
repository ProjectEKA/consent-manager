package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.model.CareContext;
import in.projecteka.consentmanager.clients.model.Patient;

import java.util.List;
import java.util.stream.Collectors;

public class Transformer {
    private static List<CareContext> toHIPCareContext(List<in.projecteka.consentmanager.link.link.model.CareContext> careContexts) {
        return careContexts.stream().map(careContext -> new CareContext(careContext.getReferenceNumber())).collect(Collectors.toList());
    }

    public static Patient toHIPPatient(String patientId, in.projecteka.consentmanager.link.link.model.Patient patient) {
        List<in.projecteka.consentmanager.link.link.model.CareContext> careContexts = patient.getCareContexts();
        List<CareContext> careContextsInHIP = toHIPCareContext(careContexts);
        return new Patient(patientId, patient.getReferenceNumber(), careContextsInHIP);
    }
}
