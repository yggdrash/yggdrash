/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "yggdrash.node", ignoreUnknownFields = false)
public class NodeProperties {
    private final Grpc grpc = new Grpc();
    private List<String> seedPeerList;
    private List<String> broadcastPeerList;
    private int maxPeers;
    private boolean seed;
    private boolean validator;
    private boolean delivery;

    private final Chain chain = new Chain();

    public Chain getChain() {
        return chain;
    }

    public Grpc getGrpc() {
        return grpc;
    }

    public List<String> getSeedPeerList() {
        return seedPeerList;
    }

    public void setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
    }

    public List<String> getBroadcastPeerList() {
        return broadcastPeerList;
    }

    public void setBroadcastPeerList(List<String> broadcastPeerList) {
        this.broadcastPeerList = broadcastPeerList;
    }

    public void setMaxPeers(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    public int getMaxPeers() {
        return maxPeers;
    }

    public boolean isSeed() {
        return seed;
    }

    public void setSeed(boolean seed) {
        this.seed = seed;
    }

    public boolean isValidator() {
        return validator;
    }

    public void setValidator(boolean validator) {
        this.validator = validator;
    }

    public boolean isDelivery() {
        return delivery;
    }

    public void setDelivery(boolean delivery) {
        this.delivery = delivery;
    }

    public static class Chain {
        private boolean enabled;
        private boolean gen;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isGen() {
            return gen;
        }

        public void setGen(boolean gen) {
            this.gen = gen;
        }
    }

    public static class Grpc {
        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
