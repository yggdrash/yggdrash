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
import io.yggdrash.contract.GenesisFrontierParam;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchInfo {
    public String chain;
    public String version;
    public String type;
    public String prevBlockHash;
    public String index;
    public String timestamp;
    public String merkleRoot;
    public String bodyLength;
    public String signature;
    public List<BranchData> body;

    public BranchInfo() {
    }

    public BranchId getBranchId() {
        return new BranchId(new Sha3Hash(toString().getBytes()));
    }

    @Override
    public String toString() {
        return "BranchInfo{"
                + "chain='" + chain + '\''
                + ", version='" + version + '\''
                + ", type='" + type + '\''
                + ", prevBlockHash='" + prevBlockHash + '\''
                + ", index='" + index + '\''
                + ", timestamp='" + timestamp + '\''
                + ", merkleRoot='" + merkleRoot + '\''
                + ", bodyLength='" + bodyLength + '\''
                + ", signature='" + signature + '\''
                + ", body='" + body + '\''
                + '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BranchData {
        public String chain;
        public String version;
        public String type;
        public String timestamp;
        public String bodyHash;
        public String bodyLength;
        public String signature;
        public GenesisFrontierParam body;
    }
}
