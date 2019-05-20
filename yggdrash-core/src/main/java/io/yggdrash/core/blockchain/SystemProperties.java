package io.yggdrash.core.blockchain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SystemProperties {
    private String elasticsearchHost;
    private int elasticsearchPort;
    private String[] eventStore;

    public String getElasticsearchHost() {
        return elasticsearchHost;
    }

    public int getElasticsearchPort() {
        return elasticsearchPort;
    }

    public String getElasticsearchAddress() {
        return String.format("%s:%d", elasticsearchHost, elasticsearchPort);
    }

    public Set<String> getEventStore() {
        if (eventStore == null) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(eventStore));
    }

    public static final class SystemPropertiesBuilder {
        private String elasticsearchHost;
        private int elasticsearchPort;
        private String[] eventStore;

        private SystemPropertiesBuilder() {
        }

        public SystemPropertiesBuilder setElasticsearchHost(String elasticsearchHost) {
            this.elasticsearchHost = elasticsearchHost;
            return this;
        }

        public SystemPropertiesBuilder setElasticsearchPort(int elasticsearchPort) {
            this.elasticsearchPort = elasticsearchPort;
            return this;
        }

        public SystemPropertiesBuilder setEventStore(String[] eventStore) {
            this.eventStore = eventStore;
            return this;
        }

        public SystemProperties build() {
            SystemProperties systemProperties = new SystemProperties();
            systemProperties.elasticsearchHost = this.elasticsearchHost;
            systemProperties.elasticsearchPort = this.elasticsearchPort;
            systemProperties.eventStore = this.eventStore;
            return systemProperties;
        }

        public static SystemPropertiesBuilder newBuilder() {
            return new SystemPropertiesBuilder();
        }
    }
}
