package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.model.Address;
import in.projecteka.consentmanager.link.discovery.model.Coding;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Telecom;

public class Transformer {

    private Transformer() {
    }

    public static ProviderRepresentation to(Provider provider) {
        var address = provider.getAddresses()
                .stream()
                .filter(add -> add.getUse().equalsIgnoreCase("work"))
                .findFirst()
                .orElse(!provider.getAddresses().isEmpty() ?
                        provider.getAddresses().get(0)
                        : new Address("", ""));
        var telecommunication = provider.getTelecoms()
                .stream()
                .filter(tel -> tel.getUse().equalsIgnoreCase("work"))
                .findFirst()
                .orElse(!provider.getTelecoms().isEmpty() ?
                        provider.getTelecoms().get(0)
                        : new Telecom("", ""));
        return ProviderRepresentation.builder()
                .city(address.getCity())
                .telephone(telecommunication.getValue())
                .type(typeFrom(provider))
                .identifier(identifierFrom(provider))
                .build();
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
                .map(type -> type.getCoding().stream().findFirst().orElse(new Coding()).getCode())
                .orElse("");
    }
}