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
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.blockchain.Log;
import io.yggdrash.core.exception.DecodeException;
import io.yggdrash.core.exception.NonExistObjectException;

import java.util.List;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

@JsonRpcService("/api/log")
public interface LogApi {

    /**
     * Returns the log of the index
     *
     * @param index logIndex
     * @return corresponding log
     */
    @JsonRpcErrors({@JsonRpcError(exception = NonExistObjectException.class, code = NonExistObjectException.CODE),
            @JsonRpcError(exception = DecodeException.class, code = DecodeException.CODE)})
    Log getLog(@JsonRpcParam(value = BRANCH_ID) String branchId,
               @JsonRpcParam(value = "index") long index);

    /**
     * Returns the logs of offset from start
     *
     * @param branchId branchId
     * @param start  start index
     * @param offset offset
     * @return corresponding logs
     */
    @JsonRpcErrors({@JsonRpcError(exception = NonExistObjectException.class, code = NonExistObjectException.CODE),
            @JsonRpcError(exception = DecodeException.class, code = DecodeException.CODE)})
    List<Log> getLogs(@JsonRpcParam(value = BRANCH_ID) String branchId,
                      @JsonRpcParam(value = "start") long start,
                      @JsonRpcParam(value = "offset") long offset);

    /**
     * Returns the logs of offset from start filtered by regex
     *
     * @param branchId branchId
     * @param regex    logs filter condition
     * @param start    start index
     * @param offset   offset
     * @return corresponding filtered logs
     */
    @JsonRpcErrors({@JsonRpcError(exception = NonExistObjectException.class, code = NonExistObjectException.CODE),
            @JsonRpcError(exception = DecodeException.class, code = DecodeException.CODE)})
    List<Log> getLogs(@JsonRpcParam(value = BRANCH_ID) String branchId,
                      @JsonRpcParam(value = "regex") String regex,
                      @JsonRpcParam(value = "start") long start,
                      @JsonRpcParam(value = "offset") long offset);

    /**
     * Returns the current index of logStore
     *
     * @param branchId branchId
     * @return current index
     */
    @JsonRpcErrors({@JsonRpcError(exception = NonExistObjectException.class, code = NonExistObjectException.CODE),
            @JsonRpcError(exception = DecodeException.class, code = DecodeException.CODE)})
    long curIndex(@JsonRpcParam(value = BRANCH_ID) String branchId);

}