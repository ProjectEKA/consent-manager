package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.discovery.model.*;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    static Telecom.TelecomBuilder telecom() {
        return easyRandom.nextObject(Telecom.TelecomBuilder.class);
    }

    static Provider.ProviderBuilder provider() {
        return easyRandom.nextObject(Provider.ProviderBuilder.class);
    }

    static Type.TypeBuilder type() {
        return easyRandom.nextObject(Type.TypeBuilder.class);
    }

    static Coding.CodingBuilder coding() {
        return easyRandom.nextObject(Coding.CodingBuilder.class);
    }

    static Address.AddressBuilder address() {
        return easyRandom.nextObject(Address.AddressBuilder.class);
    }

    static Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
    }

}
