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

package io.yggdrash.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.yggdrash.common.Sha3Hash;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchInfo {
    public String type;
    public String version;
    public String timestamp;
    public String prevBlockHash;
    public String merkleRoot;
    public String dataSize;
    public String signature;
    public List<BranchData> data;

    public ChainId getChainId() {
        return new ChainId(new Sha3Hash(toString().getBytes()));
    }

    @Override
    public String toString() {
        return "BranchInfo{"
                + "type='" + type + '\''
                + ", version='" + version + '\''
                + ", prevBlockHash='" + prevBlockHash + '\''
                + ", merkleRoot='" + merkleRoot + '\''
                + ", timestamp='" + timestamp + '\''
                + ", dataSize='" + dataSize + '\''
                + ", signature='" + signature + '\''
                + ", data='" + data + '\''
                + '}';
    }

    public static class BranchData {
        public String type;
        public String version;
        public String dataHash;
        public String timestamp;
        public String dataSize;
        public String signature;
        public String data;
    }
}
