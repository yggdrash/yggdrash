/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.gateway.dto.NodeStatusDto;

@JsonRpcService("/api/node")
public interface NodeApi {

    /**
     * Returns the node status
     *
     * @return node status
     */
    @JsonRpcErrors( {
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    NodeStatusDto getNodeStatus();

}