package in.projecteka.hdaf.link.discovery;

import in.projecteka.hdaf.link.TestBuilders;
import in.projecteka.hdaf.link.discovery.model.Address;
import in.projecteka.hdaf.link.discovery.model.Coding;
import in.projecteka.hdaf.link.discovery.model.Identifier;
import in.projecteka.hdaf.link.discovery.model.Telecom;
import in.projecteka.hdaf.link.discovery.model.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransformerTest {

    @Test
    void shouldTransformProviderToProviderRepresentation() {
        Identifier identifier = TestBuilders.identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).build();
        Address address = TestBuilders.address().build();
        Telecom telecom = TestBuilders.telecom().build();
        Coding coding = TestBuilders.coding().build();
        Type type = TestBuilders.type().coding(List.of(coding)).build();
        var provider = TestBuilders.provider()
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
        assertThat(providerRepresentation.getIdentifier().getId()).isEqualTo(identifier.getType());
    }

    @Test
    void pickWorkTelephoneWhenAvailable() {
        Telecom work = TestBuilders.telecom().use("work").build();
        Telecom another = TestBuilders.telecom().build();
        var provider = TestBuilders.provider()
                .telecoms(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo(work.getValue());
    }

    @Test
    void pickFirstWorkTelephoneWhenMultipleAvailable() {
        Telecom work = TestBuilders.telecom().use("work").build();
        Telecom another = TestBuilders.telecom().use("work").build();
        var provider = TestBuilders.provider()
                .telecoms(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo(another.getValue());
    }

    @Test
    void returnsEmptyTelephoneWhenSourceIsEmpty() {
        var provider = TestBuilders.provider()
                .telecoms(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo("");
    }

    @Test
    void pickWorkCityWhenAvailable() {
        Address work = TestBuilders.address().use("work").build();
        Address another = TestBuilders.address().build();
        var provider = TestBuilders.provider()
                .addresses(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(work.getCity());
    }

    @Test
    void pickFirstWorkCityWhenMultipleAvailable() {
        Address work = TestBuilders.address().use("work").build();
        Address another = TestBuilders.address().use("Work").build();
        var provider = TestBuilders.provider()
                .addresses(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(another.getCity());
    }

    @Test
    void returnsEmptyCityWhenSourceIsEmpty() {
        var provider = TestBuilders.provider()
                .addresses(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo("");
    }

    @Test
    void returnsEmptyIdentifierWhenOfficialIsUnavailable() {
        var provider = TestBuilders.provider()
                .identifiers(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getIdentifier()).isEqualTo(IdentifierRepresentation.builder().build());
    }

    @Test
    void pickWorkCityAndWorkTelephoneWhenAvailable() {
        Address address = TestBuilders.address().use("work").build();
        Address anotherAddress = TestBuilders.address().build();
        Telecom telecom = TestBuilders.telecom().use("work").build();
        Telecom anotherTelecom = TestBuilders.telecom().build();
        var provider = TestBuilders.provider()
                .addresses(List.of(address, anotherAddress))
                .telecoms(List.of(telecom, anotherTelecom))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(address.getCity());
        assertThat(providerRepresentation.getTelephone()).isEqualTo(telecom.getValue());
    }
}