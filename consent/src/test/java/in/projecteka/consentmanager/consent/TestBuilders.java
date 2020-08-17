package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.CertResponse;
import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.FetchRequest;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactLightRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResult;
import in.projecteka.consentmanager.consent.model.response.HIPCosentNotificationAcknowledgment;
import org.jeasy.random.EasyRandom;

public class TestBuilders {
    private static final EasyRandom easyRandom = new EasyRandom();

    public static ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder consentArtefactRepresentation() {
        return easyRandom.nextObject(ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder.class);
    }

    public static ConsentArtefact.ConsentArtefactBuilder consentArtefact() {
        return easyRandom.nextObject(ConsentArtefact.ConsentArtefactBuilder.class);
    }

    public static ConsentRepresentation.ConsentRepresentationBuilder consentRepresentation() {
        return easyRandom.nextObject(ConsentRepresentation.ConsentRepresentationBuilder.class);
    }

    public static ConsentRequestDetail.ConsentRequestDetailBuilder consentRequestDetail() {
        return easyRandom.nextObject(ConsentRequestDetail.ConsentRequestDetailBuilder.class);
    }

    public static HIPConsentArtefactRepresentation.HIPConsentArtefactRepresentationBuilder hipConsentArtefactRepresentation() {
        return easyRandom.nextObject(HIPConsentArtefactRepresentation.HIPConsentArtefactRepresentationBuilder.class);
    }

    public static ConsentArtefactLightRepresentation.ConsentArtefactLightRepresentationBuilder artefactLightRepresentation() {
        return easyRandom.nextObject(ConsentArtefactLightRepresentation.ConsentArtefactLightRepresentationBuilder.class);
    }

    public static ConsentRequestResult.ConsentRequestResultBuilder consentRequestResult() {
        return easyRandom.nextObject(ConsentRequestResult.ConsentRequestResultBuilder.class);
    }

    public static ConsentRequest.ConsentRequestBuilder consentRequest() {
        return easyRandom.nextObject(ConsentRequest.ConsentRequestBuilder.class);
    }
    public static FetchRequest.FetchRequestBuilder fetchRequest() {
        return easyRandom.nextObject(FetchRequest.FetchRequestBuilder.class);
    }

    public static HIPCosentNotificationAcknowledgment.HIPCosentNotificationAcknowledgmentBuilder hipConsentNotificationAcknowledgement() {
        return easyRandom.nextObject(HIPCosentNotificationAcknowledgment.HIPCosentNotificationAcknowledgmentBuilder.class);
    }

    public static CertResponse.CertResponseBuilder certResponse() {
        return easyRandom.nextObject(CertResponse.CertResponseBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }
}
