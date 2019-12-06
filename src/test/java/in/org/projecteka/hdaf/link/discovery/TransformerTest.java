package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.discovery.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static in.org.projecteka.hdaf.link.discovery.TestBuilders.*;
import static org.assertj.core.api.Assertions.assertThat;

class TransformerTest {


    @Test
    public void shouldTransformProviderToProviderRepresentation() {
        Address address = address().build();
        Telecom telecom = telecom().build();
        Coding coding = coding().build();
        Type type = type().coding(List.of(coding)).build();
        var provider = provider()
                .addresses(List.of(address))
                .telecoms(List.of(telecom))
                .types(List.of(type))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getName()).isEqualTo(provider.getName());
        assertThat(providerRepresentation.getCity()).isEqualTo(address.getCity());
        assertThat(providerRepresentation.getTelephone()).isEqualTo(telecom.getValue());
        assertThat(providerRepresentation.getType()).isEqualTo(coding.getCode());
    }

    @Test
    public void pickWorkTelephoneWhenAvailable() {
        Telecom work = telecom().use("work").build();
        Telecom another = telecom().build();
        var provider = provider()
                .telecoms(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo(work.getValue());
    }

    @Test
    public void pickFirstWorkTelephoneWhenMultipleAvailable() {
        Telecom work = telecom().use("work").build();
        Telecom another = telecom().use("work").build();
        var provider = provider()
                .telecoms(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo(another.getValue());
    }

    @Test
    public void returnsEmptyTelephoneWhenSourceIsEmpty() {
        var provider = provider()
                .telecoms(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getTelephone()).isEqualTo("");
    }

    @Test
    public void pickWorkCityWhenAvailable() {
        Address work = address().use("work").build();
        Address another = address().build();
        var provider = provider()
                .addresses(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(work.getCity());
    }

    @Test
    public void pickFirstWorkCityWhenMultipleAvailable() {
        Address work = address().use("work").build();
        Address another = address().use("Work").build();
        var provider = provider()
                .addresses(List.of(another,work))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(another.getCity());
    }

    @Test
    public void returnsEmptyCityWhenSourceIsEmpty() {
        var provider = provider()
                .addresses(List.of())
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo("");
    }

    @Test
    public void pickWorkCityAndWorkTelphonewhenAvailable() {
        Address address = address().use("work").build();
        Address anotherAddress = address().build();
        Telecom telcom = telecom().use("work").build();
        Telecom anotherTelcom = telecom().build();
        var provider = provider()
                .addresses(List.of(address, anotherAddress))
                .telecoms(List.of(telcom, anotherTelcom))
                .build();

        ProviderRepresentation providerRepresentation = Transformer.to(provider);

        assertThat(providerRepresentation.getCity()).isEqualTo(address.getCity());
        assertThat(providerRepresentation.getTelephone()).isEqualTo(telcom.getValue());
    }


}