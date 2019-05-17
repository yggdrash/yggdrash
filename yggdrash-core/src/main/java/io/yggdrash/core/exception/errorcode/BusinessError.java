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

package io.yggdrash.core.exception.errorcode;

import java.util.ArrayList;
import java.util.List;

public enum BusinessError {
    //The errors are appended to the transactionResponseDto.
    VALID(32000),                        //111110100000000
    UNTRUSTED(32001),                    //111110100000001
    REQUEST_TIMEOUT(32002),              //111110100000010
    UNKNOWN_BLOCK_HEIGHT(32004),         //111110100000100
    INVALID_MERKLE_ROOT_HASH(32008),     //111110100001000
    DUPLICATED(32016),                   //111110100010000
    INVALID_DATA_FORMAT(32032),          //111110100100000
    INVALID_BLOCK_HASH(32064)            //111110101000000
    ;

    private int code;

    BusinessError(int code) {
        this.code = code;
    }

    public static int addCode(boolean flag, BusinessError appendCode) {
        // flash is false
        if (!flag) {
            return appendCode.code;
        }
        return VALID.code;
    }

    public int toValue() {
        return code;
    }

    public static List<String> errorLogs(int code) {
        List<String> errorLogs = new ArrayList<>();

        if ((code & UNTRUSTED.code) == UNTRUSTED.code) {
            errorLogs.add("Untrusted");
        }
        if ((code & REQUEST_TIMEOUT.code) == REQUEST_TIMEOUT.code) {
            errorLogs.add("Request Timeout");
        }
        if ((code & UNKNOWN_BLOCK_HEIGHT.code) == UNKNOWN_BLOCK_HEIGHT.code) {
            errorLogs.add("Unknown BlockHeight");
        }
        if ((code & INVALID_MERKLE_ROOT_HASH.code) == INVALID_MERKLE_ROOT_HASH.code) {
            errorLogs.add("Unknown BlockBody Hash");
        }
        if ((code & DUPLICATED.code) == DUPLICATED.code) {
            errorLogs.add("Duplicated");
        }
        if ((code & INVALID_DATA_FORMAT.code) == INVALID_DATA_FORMAT.code) {
            errorLogs.add("Invalid data format");
        }
        if ((code & INVALID_BLOCK_HASH.code) == INVALID_BLOCK_HASH.code) {
            errorLogs.add("Invalid BlockHash");
        }

        return errorLogs;
    }
}