package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.discovery.model.*;

public class Transformer {
  public static ProviderRepresentation to(Provider provider) {
    Address address = provider.getAddresses()
        .stream()
        .filter(add -> add.getUse().equalsIgnoreCase("work"))
        .findFirst()
        .orElse(provider.getAddresses().size() > 0 ?
            provider.getAddresses().get(0)
            : new Address("", ""));
    Telecom telecom = provider.getTelecoms()
        .stream()
        .filter(tel -> tel.getUse().equalsIgnoreCase("work"))
        .findFirst()
        .orElse(provider.getTelecoms().size() > 0 ?
            provider.getTelecoms().get(0)
            : new Telecom("", ""));
    return ProviderRepresentation.builder()
        .city(address.getCity())
        .name(provider.getName())
        .telephone(telecom.getValue())
        .type(from(provider))
        .build();
  }

  private static String from(Provider provider) {
    return provider.getTypes().stream()
        .findFirst()
        .map(type -> type.getCoding().stream().findFirst().orElse(new Coding()).getCode())
        .orElse("");
  }
}