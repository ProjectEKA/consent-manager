package in.projecteka.consentmanager.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public final class Serializer {

    private Serializer() {

    }

    private static final ObjectMapper mapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public static <T> String from(T data) {
        return mapper.writeValueAsString(data);
    }

    @SneakyThrows
    public static <T> T to(String value, Class<T> type) {
        return mapper.readValue(value.getBytes(), type);
    }
}
