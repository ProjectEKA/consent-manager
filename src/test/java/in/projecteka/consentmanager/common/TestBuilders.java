package in.projecteka.consentmanager.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jeasy.random.EasyRandom;

public class TestBuilders {
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private static final EasyRandom easyRandom = new EasyRandom();

	public static String string() {
		return easyRandom.nextObject(String.class);
	}
}
