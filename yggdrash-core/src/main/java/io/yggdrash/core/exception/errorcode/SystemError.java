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

public enum SystemError {
    //Errors are included in the exception handling message.
    VALID(33000),                      //1000000011101000
    BRANCH_NOT_FOUND(33001),           //1000000011101001
    CONTRACT_VERSION_NOT_FOUND(33002)  //1000000011101010
    ;
    private int code;

    SystemError(int code) {
        this.code = code;
    }

    public static int addCode(boolean flag, SystemError appendCode) {
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
        if ((code & BRANCH_NOT_FOUND.code) == BRANCH_NOT_FOUND.code) {
            errorLogs.add("Branch doesn't exist");
        }
        if ((code & CONTRACT_VERSION_NOT_FOUND.code) == CONTRACT_VERSION_NOT_FOUND.code) {
            errorLogs.add("ContractVersion doesn't exist");
        }

        return errorLogs;
    }
}