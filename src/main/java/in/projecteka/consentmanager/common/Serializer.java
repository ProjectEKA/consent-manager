package in.projecteka.consentmanager.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class Serializer {
    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);

    private Serializer() {

    }

    private static final ObjectMapper mapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public static <T> String from(T data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error("Can not serialize data", e);
            return null;
        }
    }

    @SneakyThrows
    public static <T> T to(String value, Class<T> type) {
        return mapper.readValue(value.getBytes(), type);
    }

    public static <T> Optional<T> tryTo(String value, Class<T> type) {
        try {
            return Optional.ofNullable(mapper.readValue(value, type));
        } catch (JsonProcessingException e) {
            logger.error("Can not deserialize data", e);
            return Optional.empty();
        }
    }
}
