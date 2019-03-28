package io.yggdrash.core.blockchain;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SystemProperties {
    private String esHost;
    private String esTransport;
    private String[] eventStore;

    boolean checkEsClient() {
        return !StringUtils.isEmpty(esHost)
                && !StringUtils.isEmpty(esTransport)
                && eventStore != null
                && eventStore.length > 0;
    }

    private String[] splitHost() {
        String[] splitHost = null;
        if (esHost != null) {
            splitHost = esHost.split(":");
            if (splitHost.length != 2) {
                throw new IllegalArgumentException("The es.host value must be of the form ip:port.");
            }
        }

        return splitHost;
    }

    public String getEsHost() {
        return esHost;
    }

    String getEsPrefixHost() {
        String[] splitHost = splitHost();
        if (splitHost == null) {
            return null;
        }
        return splitHost[0];
    }

    public int getEsPort() {
        String[] splitHost = splitHost();
        if (splitHost == null) {
            return 0;
        }
        return Integer.parseInt(splitHost[1]);
    }

    int getEsTransport() {
        if (!StringUtils.isNumeric(esTransport)) {
            throw new IllegalArgumentException("The es.transport value must be a number.");
        }
        return Integer.parseInt(esTransport);
    }

    public Set<String> getEventStore() {
        if (eventStore == null) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(eventStore));
    }

    public static final class SystemPropertiesBuilder {
        private String esHost;
        private String esTransport;
        private String[] eventStore;

        private SystemPropertiesBuilder() {
        }

        public static SystemPropertiesBuilder aSystemProperties() {
            return new SystemPropertiesBuilder();
        }

        public SystemPropertiesBuilder withEsHost(String esHost) {
            this.esHost = esHost;
            return this;
        }

        public SystemPropertiesBuilder withEsTransport(String esTransport) {
            this.esTransport = esTransport;
            return this;
        }

        public SystemPropertiesBuilder withEventStore(String[] eventStore) {
            this.eventStore = eventStore;
            return this;
        }

        public SystemProperties build() {
            SystemProperties systemProperties = new SystemProperties();
            systemProperties.eventStore = this.eventStore;
            systemProperties.esTransport = this.esTransport;
            systemProperties.esHost = this.esHost;
            return systemProperties;
        }
    }
}
