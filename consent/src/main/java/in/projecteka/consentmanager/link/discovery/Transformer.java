package in.projecteka.consentmanager.link.discovery;

import in.projecteka.library.clients.model.Address;
import in.projecteka.library.clients.model.Coding;
import in.projecteka.library.clients.model.Identifier;
import in.projecteka.library.clients.model.Provider;
import in.projecteka.library.clients.model.Telecom;

public class Transformer {

    private Transformer() {
    }

    public static ProviderRepresentation to(Provider provider) {
        return ProviderRepresentation.builder()
                .city(cityFrom(provider))
                .telephone(telephoneFrom(provider))
                .type(typeFrom(provider))
                .identifier(identifierFrom(provider))
                .build();
    }

    private static String telephoneFrom(Provider provider) {
        if (provider.getTelecoms() != null) {
            var telecom = provider.getTelecoms()
                    .stream()
                    .filter(tel -> tel.getUse().equalsIgnoreCase("work"))
                    .findFirst()
                    .orElse(!provider.getTelecoms().isEmpty()
                            ? provider.getTelecoms().get(0)
                            : new Telecom("", ""));
            return telecom.getValue();
        }
        return null;
    }

    private static String cityFrom(Provider provider) {
        if (provider.getAddresses() != null) {
            var address = provider.getAddresses()
                    .stream()
                    .filter(add -> add.getUse().equalsIgnoreCase("work"))
                    .findFirst()
                    .orElse(!provider.getAddresses().isEmpty()
                            ? provider.getAddresses().get(0)
                            : new Address("", ""));
            return address.getCity();
        }
        return null;
    }

    private static IdentifierRepresentation identifierFrom(Provider provider) {
        return provider.getIdentifiers()
                .stream()
                .filter(Identifier::isOfficial)
                .findFirst()
                .map(identifier -> new IdentifierRepresentation(provider.getName(), identifier.getValue()))
                .orElse(IdentifierRepresentation.builder().build());
    }

    private static String typeFrom(Provider provider) {
        return provider.getTypes().stream()
                .findFirst()
                .map(type -> type.getCoding().stream().findFirst().orElse(new Coding("")).getCode())
                .orElse("");
    }
}