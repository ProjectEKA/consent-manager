package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkedCareContexts {
    private class CareContext {
        String careContextReference;
        String display;
    }

    private class HipLink {
        String hipId;
        String hipName;
        String patientReference;
        List<CareContext> careContexts = new ArrayList<>();
    }

    private List<HipLink> links = new ArrayList<>();

    @JsonProperty("patient")
    private void setPatientLinks(Map<String,Object> patientMap) {
        Object links = patientMap.get("links");
        if ((links != null) && (links instanceof List)) {
            ((List) links).stream().forEach( link -> {
                HipLink hipLink = null;
                Object hipMap = ((Map) link).get("hip");
                if (hipMap != null && hipMap instanceof Map) {
                    hipLink = new HipLink();
                    hipLink.hipId = (String) ((Map) hipMap).get("id");
                    hipLink.hipName = (String) ((Map) hipMap).get("name");
                    hipLink.patientReference = (String) ((Map) hipMap).get("referenceNumber");

                }
                Object careContexts = ((Map) link).get("careContexts");
                if (careContexts instanceof List) {
                    Iterator iterator = ((List) careContexts).iterator();
                    while (iterator.hasNext()) {
                        Map cc = (Map) iterator.next();
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

    public boolean hasHipReference(String hipId) {
        for (HipLink link : links) {
            if (link.hipId.equalsIgnoreCase(hipId)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCCReference(String hipId, String ccReference) {
        Optional<HipLink> hipLink = links.stream().filter(aLink -> {
            return aLink.hipId.equalsIgnoreCase(hipId);
        }).findFirst();

        if (hipLink.isPresent()) {
            Optional<CareContext> patientCC = hipLink.get().careContexts.stream().filter(cc -> {
                return cc.careContextReference.equalsIgnoreCase(ccReference);
            }).findFirst();
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
}
