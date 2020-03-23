package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.Notification;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import org.jeasy.random.EasyRandom;

public class TestBuilders {
    private static final EasyRandom easyRandom = new EasyRandom();

    public static ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder consentArtefactRepresentation() {
        return easyRandom.nextObject(ConsentArtefactRepresentation.ConsentArtefactRepresentationBuilder.class);
    }

    public static ConsentRepresentation.ConsentRepresentationBuilder consentRepresentation() {
        return easyRandom.nextObject(ConsentRepresentation.ConsentRepresentationBuilder.class);
    }

    public static Notification.NotificationBuilder notificationMessage() {
        return easyRandom.nextObject(Notification.NotificationBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }
}
