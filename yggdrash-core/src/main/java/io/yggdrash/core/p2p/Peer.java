/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.p2p;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.exception.NotValidateException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class Peer {
    private static final String YGGDRASH_NODE_SCHEMA = "ynode";
    private static final String PEER_URI_FORMAT = "%s://%s@%s";
    private PeerId peerId;
    private Sha3Hash pubKey;
    private String host;
    private int port;
    private String ynodeUri;
    private long bestBlock;
    private long modified;
    private int distance;

    private Peer(String ynodeUri) {
        try {
            URI uri = new URI(ynodeUri);
            if (!uri.getScheme().equals(YGGDRASH_NODE_SCHEMA)) {
                throw new NotValidateException(
                        "expecting URL in the format ynode://PUBKEY@HOST:PORT");
            }
            this.peerId = PeerId.of(ynodeUri);
            if (uri.getUserInfo() != null) {
                this.pubKey = new Sha3Hash(uri.getUserInfo());
            }
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.ynodeUri = ynodeUri;
            touch();
        } catch (URISyntaxException e) {
            throw new NotValidateException("expecting URL in the format ynode://PUBKEY@HOST:PORT");
        }
    }

    public static Peer valueOf(String ynodeUri) {
        return new Peer(ynodeUri);
    }

    public static Peer valueOf(byte[] ynodeUriBytes) {
        return valueOf(new String(ynodeUriBytes));
    }

    public static Peer valueOf(String nodeId, String host, int port) {
        return valueOf(String.format(PEER_URI_FORMAT, YGGDRASH_NODE_SCHEMA, nodeId, host + ":" + port));
    }

    public PeerId getPeerId() {
        return peerId;
    }

    public Sha3Hash getPubKey() {
        return pubKey;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getYnodeUri() {
        return ynodeUri;
    }

    public long getBestBlock() {
        return bestBlock;
    }

    public void setBestBlock(long bestBlock) {
        this.bestBlock = bestBlock;
    }

    void setDistance(Peer owner) {
        this.distance = owner.peerId.distanceTo(peerId.getBytes());
    }

    boolean isLocal() {
        return host.equals("127.0.0.1") || host.equals("localhost");
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(peerId.getBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Peer peer = (Peer) o;
        return peerId.equals(peer.getPeerId());
    }

    @Override
    public String toString() {
        return ynodeUri;
    }

    public String toAddress() {
        return host + ":" + port;
    }

    void touch() {
        modified = System.currentTimeMillis();
    }

    long getModified() {
        return modified;
    }

    int getDistance() {
        return distance;
    }
}
