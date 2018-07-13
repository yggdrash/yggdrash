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

package io.yggdrash.core.net;

import io.yggdrash.util.Utils;
import org.spongycastle.util.encoders.Hex;

import java.net.URI;
import java.net.URISyntaxException;

public class Peer {
    public static final String YEED_PEER_SCHEMA = "ynode";

    private byte[] id;
    String host;
    int port;

    public Peer(String host, int port) {
        this.id = "node".getBytes();
        this.host = host;
        this.port = port;
    }

    public Peer(String enodeURL) {
        try {
            URI uri = new URI(enodeURL);
            if (!uri.getScheme().equals(YEED_PEER_SCHEMA)) {
                throw new RuntimeException("expecting URL in the format ynode://PUBKEY@HOST:PORT");
            }
            this.id = Hex.decode(uri.getUserInfo());
            this.host = uri.getHost();
            this.port = uri.getPort();
        } catch (URISyntaxException e) {
            throw new RuntimeException("expecting URL in the format ynode://PUBKEY@HOST:PORT", e);
        }
    }

    public String getId() {
        return Utils.getNodeIdShort(getHexId());
    }

    public String getHexId() {
        return Hex.toHexString(id);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

}
