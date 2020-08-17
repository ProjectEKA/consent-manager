package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkedCareContexts {
    private static class CareContext {
        String careContextReference;
        String display;
    }

    private static class HipLink {
        String hipId;
        String hipName;
        String patientReference;
        List<CareContext> careContexts = new ArrayList<>();
    }

    @Builder
    @AllArgsConstructor
    @Value
    public static class PatientLinkedCareContext {
        private String patientRefNo;
        private String careContextRefNo;
    }

    private final List<HipLink> links = new ArrayList<>();

    @JsonProperty("patient")
    private void setPatientLinks(Map<String, Object> patientMap) {
        Object links = patientMap.get("links");
        if ((links instanceof List)) {
            ((List) links).forEach(link -> {
                HipLink hipLink = null;
                Object hipMap = ((Map) link).get("hip");
                if (hipMap instanceof Map) {
                    hipLink = new HipLink();
                    hipLink.hipId = (String) ((Map) hipMap).get("id");
                    hipLink.hipName = (String) ((Map) hipMap).get("name");
                    hipLink.patientReference = (String) ((Map) link).get("referenceNumber");

                }
                Object careContexts = ((Map) link).get("careContexts");
                if (careContexts instanceof List) {
                    for (Object o : (List) careContexts) {
                        Map cc = (Map) o;
                        if (hipLink != null) {
                            CareContext careContext = new CareContext();
                            careContext.careContextReference = (String) cc.get("referenceNumber");
                            careContext.display = (String) cc.get("display");
                            hipLink.careContexts.add(careContext);
                        }
                    }
                }
                if (hipLink != null) {
                    this.links.add(hipLink);
                }
            });
        }
    }

    private boolean hasHipReference(String hipId) {
        for (HipLink link : links) {
            if (link.hipId.equalsIgnoreCase(hipId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCCReference(String hipId, String ccReference) {
        Optional<HipLink> hipLink = links.stream().filter(aLink -> aLink.hipId.equalsIgnoreCase(hipId)).findFirst();

        if (hipLink.isPresent()) {
            Optional<CareContext> patientCC = hipLink.get()
                    .careContexts
                    .stream()
                    .filter(cc -> cc.careContextReference.equalsIgnoreCase(ccReference))
                    .findFirst();
            return patientCC.isPresent();
        }
        return false;
    }

    public boolean hasCCReferences(String hipId, List<String> ccRefs) {
        boolean result = hasHipReference(hipId);
        if (result) {
            for (String ccRef : ccRefs) {
                result = result && hasCCReference(hipId, ccRef);
            }
        }
        return result;
    }

    public List<PatientLinkedCareContext> getCareContext(String hipId) {
        return links.stream().filter(hipLink -> hipLink.hipId.equals(hipId))
                .flatMap(hipLink -> hipLink.careContexts.stream().map(careContext ->
                        PatientLinkedCareContext.builder()
                                .careContextRefNo(careContext.careContextReference)
                                .patientRefNo(hipLink.patientReference)
                                .build())).collect(Collectors.toList());
    }
}
