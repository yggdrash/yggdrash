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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum BusinessError {
    //The errors are appended to the transactionResponseDto.
    VALID(32000),                       //0x7d00, 0111 1101 0000 0000
    UNTRUSTED(32001),                   //0x7d01, 0111 1101 0000 0001
    REQUEST_TIMEOUT(32002),             //0x7d02, 0111 1101 0000 0010
    UNKNOWN_BLOCK_HEIGHT(32004),        //0x7d04, 0111 1101 0000 0100
    INVALID_MERKLE_ROOT_HASH(32008),    //0x7d08, 0111 1101 0000 1000
    DUPLICATED(32016),                  //0x7d10, 0111 1101 0001 0000
    INVALID_DATA_FORMAT(32032),         //0x7d20, 0111 1101 0010 0000
    INVALID_BLOCK_HASH(32064),          //0x7d40, 0111 1101 0100 0000
    UNDEFINED_ERROR(32128),             //0x7d80, 0111 1101 1000 0000
    INVALID_STATE_ROOT_HASH(32256)      //0x7E00, 0111 1110 0000 0000
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
        if ((code & UNDEFINED_ERROR.code) == UNDEFINED_ERROR.code) {
            errorLogs.add("Undefined Error");
        }
        if ((code & INVALID_STATE_ROOT_HASH.code) == INVALID_STATE_ROOT_HASH.code) {
            errorLogs.add("Invalid StateRoot");
        }

        return errorLogs;
    }

    public static Map<String, List<String>> getErrorLogsMap(int code) {
        Map<String, List<String>> errLogs = new HashMap<>();
        errLogs.put("BusinessError", errorLogs(code));
        return errLogs;
    }
}