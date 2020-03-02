package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.NotificationMessage;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import org.jeasy.random.EasyRandom;

public class TestBuilders {
    private static final EasyRandom easyRandom = new EasyRandom();

    public static ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder consentArtefactRepresentation() {
        return easyRandom.nextObject(ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder.class);
    }

    public static NotificationMessage.NotificationMessageBuilder notificationMessage(){
        return easyRandom.nextObject(NotificationMessage.NotificationMessageBuilder.class);
    }

}
