/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.genesis;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.BranchId;
import org.spongycastle.util.encoders.Hex;

import java.util.Map;

public class BranchJson {
    public String name;
    public String owner;
    public String symbol;
    public String property;
    public String type;
    public String timestamp;
    public String description;
    public String tag;
    public String version;
    @JsonProperty("reference_address")
    public String referenceAddress;
    @JsonProperty("reserve_address")
    public String reserveAddress;
    public Map<String, Map<String, Map<String, String>>> genesis;
    public String signature;
    @JsonProperty("branch_id")
    public String branchId;

    long longTimestamp() {
        return ByteUtil.byteArrayToLong(Hex.decode(timestamp));
    }

    BranchId branchId() {
        return BranchId.of(branchId);
    }
}
