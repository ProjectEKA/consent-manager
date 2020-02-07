package in.projecteka.consentmanager.common;

import java.util.Base64;

public class TokenUtils {
    public static String getCallerId(String authorizationHeader) {
        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(authorizationHeader));
    }

    public static String encode(String authorizationHeader) {
        Base64.Encoder encoder = Base64.getEncoder();
        return new String(encoder.encode(authorizationHeader.getBytes()));
    }
}
