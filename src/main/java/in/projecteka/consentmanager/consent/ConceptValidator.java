package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import in.projecteka.consentmanager.common.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConceptValidator implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(ConceptValidator.class);

    @Value("${consentmanager.consentservice.purposeOfUseDefUrl}")
    private Resource purposeOfUseValueSetResource;
    private Map<String, String> purposesOfUse;

    @Value("${consentmanager.consentservice.hiTypesDefUrl}")
    private Resource hiTypesValueSetResource;
    private Map<String, String> hiTypes;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.purposesOfUse = readValueSetFromResource(purposeOfUseValueSetResource);
        this.hiTypes = readValueSetFromResource(hiTypesValueSetResource);
    }


    private ConcurrentHashMap<String, String> readValueSetFromResource(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            var valueSet = result.toString(StandardCharsets.UTF_8);
            JsonNode jsonNode = Serializer.to(valueSet, JsonNode.class);
            JsonNode compose = jsonNode.get("compose");
            ConcurrentHashMap<String, String> conceptCodes = new ConcurrentHashMap<>();
            if (compose != null && compose.has("include")) {
                ArrayNode includedCodes = compose.withArray("include");
                for (JsonNode includedCode : includedCodes) {
                    ArrayNode concepts = includedCode.withArray("concept");
                    for (JsonNode concept : concepts) {
                        conceptCodes.put(concept.get("code").asText(), concept.get("display").asText());
                    }
                }
            }
            JsonNode expansion = jsonNode.get("expansion");
            if (expansion != null && expansion.has("contains")) {
                ArrayNode expandedCodes = expansion.withArray("contains");
                for (JsonNode concept : expandedCodes) {
                    conceptCodes.put(concept.get("code").asText(), concept.get("display").asText());
                }
            }
            return conceptCodes;
        } catch (IOException e) {
            logger.error("Error occurred while loading processing Valueset", e);
            throw e;
        }
    }

    public Mono<Boolean> validatePurpose(String code) {
        return Mono.just(purposesOfUse.get(code) != null);
    }


    public Mono<Boolean> validateHITypes(List<String> codes) {
        if (codes.isEmpty()) {
            return Mono.just(false);
        }
        for (String code : codes) {
            if (hiTypes.get(code) == null) {
                return Mono.just(false);
            }
        }
        return Mono.just(true);
    }
}
