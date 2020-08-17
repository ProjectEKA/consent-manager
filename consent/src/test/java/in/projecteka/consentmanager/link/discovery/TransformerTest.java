package in.projecteka.consentmanager.link.discovery;

import in.projecteka.library.clients.model.Address;
import in.projecteka.library.clients.model.Coding;
import in.projecteka.library.clients.model.Identifier;
import in.projecteka.library.clients.model.Telecom;
import in.projecteka.library.clients.model.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static in.projecteka.consentmanager.clients.TestBuilders.address;
import static in.projecteka.consentmanager.clients.TestBuilders.coding;
import static in.projecteka.consentmanager.clients.TestBuilders.identifier;
import static in.projecteka.consentmanager.clients.TestBuilders.provider;
import static in.projecteka.consentmanager.clients.TestBuilders.telecom;
import static in.projecteka.consentmanager.clients.TestBuilders.type;
import static org.assertj.core.api.Assertions.assertThat;

class TransformerTest {

    @Test
    void shouldTransformProviderToProviderRepresentation() {
        Identifier identifier = identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).build();
        Address address = address().build();
        Telecom telecom = telecom().build();
        Coding coding = coding().build();
        Type type = type().coding(List.of(coding)).build();
        var provider = provider()
                .addresses(List.of(address))
                .telecoms(List.of(telecom))
                .types(List.of(type))
                .identifiers(List.of(identifier))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getIdentifier().getName()).isEqualTo(provider.getName());
        assertThat(providerRepresentation.getCity()).isEqualTo(address.getCity());
        assertThat(providerRepresentation.getTelephone()).isEqualTo(telecom.getValue());
        assertThat(providerRepresentation.getType()).isEqualTo(coding.getCode());
        assertThat(providerRepresentation.getIdentifier().getId()).isEqualTo(identifier.getValue());
    }

    @Test
    void pickWorkTelephoneWhenAvailable() {
        Telecom work = telecom().use("work").build();
        Telecom another = telecom().build();
        var provider = provider()
                .telecoms(List.of(another, work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo(work.getValue());
    }

    @Test
    void pickFirstWorkTelephoneWhenMultipleAvailable() {
        Telecom work = telecom().use("work").build();
        Telecom another = telecom().use("work").build();
        var provider = provider()
                .telecoms(List.of(another, work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo(another.getValue());
    }

    @Test
    void returnsEmptyTelephoneWhenSourceIsEmpty() {
        var provider = provider()
                .telecoms(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo("");
    }

    @Test
    void pickWorkCityWhenAvailable() {
        Address work = address().use("work").build();
        Address another = address().build();
        var provider = provider()
                .addresses(List.of(another, work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(work.getCity());
    }

    @Test
    void pickFirstWorkCityWhenMultipleAvailable() {
        Address work = address().use("work").build();
        Address another = address().use("Work").build();
        var provider = provider()
                .addresses(List.of(another, work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(another.getCity());
    }

    @Test
    void returnsEmptyCityWhenSourceIsEmpty() {
        var provider = provider()
                .addresses(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo("");
    }

    @Test
    void returnsEmptyIdentifierWhenOfficialIsUnavailable() {
        var provider = provider()
                .identifiers(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getIdentifier()).isEqualTo(IdentifierRepresentation.builder().build());
    }

    @Test
    void pickWorkCityAndWorkTelephoneWhenAvailable() {
        Address address = address().use("work").build();
        Address anotherAddress = address().build();
        Telecom telecom = telecom().use("work").build();
        Telecom anotherTelecom = telecom().build();
        var provider = provider()
                .addresses(List.of(address, anotherAddress))
                .telecoms(List.of(telecom, anotherTelecom))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(address.getCity());
        assertThat(providerRepresentation.getTelephone()).isEqualTo(telecom.getValue());
    }
}