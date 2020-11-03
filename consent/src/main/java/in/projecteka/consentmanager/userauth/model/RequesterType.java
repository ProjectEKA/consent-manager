package in.projecteka.consentmanager.userauth.model;

import lombok.Getter;

@Getter
public enum RequesterType {
    HIU("HIU") {
        @Override
        public String getRoutingKey() {
            return "X-HIU-ID";
        }
    },
    HIP("HIP") {
        @Override
        public String getRoutingKey() {
            return "X-HIP-ID";
        }
    };

    private final String value;

    RequesterType(String val) {
        value = val;
    }

    public abstract String getRoutingKey();
}
