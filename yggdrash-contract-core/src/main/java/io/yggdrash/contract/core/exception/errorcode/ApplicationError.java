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

package io.yggdrash.contract.core.exception.errorcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ApplicationError {
    //The errors are appended to the transactionReceipt.
    VALID(34000),                   //1000010011010000
    INVALID_PARAMS(34001),          //1000010011010001
    INSUFFICIENT_FUNDS(34002),      //1000010011010010
    EXECUTION_FAILED(34004)         //1000010011010100
    ;

    private int code;

    ApplicationError(int code) {
        this.code = code;
    }

    public static int addCode(boolean flag, ApplicationError appendCode) {
        // flash is false
        if (!flag) {
            return appendCode.code;
        }
        return VALID.code;
    }

    public int toValue() {
        return code;
    }

    public String toString() {
        if (code == INVALID_PARAMS.code) {
            return "Params not allowed";
        }
        if (code == INSUFFICIENT_FUNDS.code) {
            return "Insufficient funds";
        }
        if (code == EXECUTION_FAILED.code) {
            return "Execution failed";
        }
        return "";
    }

    public static List<String> errorLogs(int code) {
        List<String> errorLogs = new ArrayList<>();

        if ((code & INVALID_PARAMS.code) == INVALID_PARAMS.code) {
            errorLogs.add(INVALID_PARAMS.toString());
        }
        if ((code & INSUFFICIENT_FUNDS.code) == INSUFFICIENT_FUNDS.code) {
            errorLogs.add(INSUFFICIENT_FUNDS.toString());
        }
        if ((code & EXECUTION_FAILED.code) == EXECUTION_FAILED.code) {
            errorLogs.add(EXECUTION_FAILED.toString());
        }

        return errorLogs;
    }

    public static Map<String, List<String>> getErrorLogsMap(List<String> logs) {
        Map<String, List<String>> errLogs = new HashMap<>();
        errLogs.put("SystemError", logs);
        return errLogs;
    }
}