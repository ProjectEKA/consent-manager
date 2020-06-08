package in.projecteka.consentmanager.user.model;

import lombok.Getter;

@Getter
public enum Requester {
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

    Requester(String val) {
        value = val;
    }

    public abstract String getRoutingKey();
}
