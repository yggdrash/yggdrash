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

package io.yggdrash.contract.core.exception;

import io.yggdrash.contract.core.exception.errorcode.ApplicationError;

public class ExecutionException extends Exception {

    public static final int CODE = ApplicationError.EXECUTION_FAILED.toValue();
    private static final String MSG = ApplicationError.EXECUTION_FAILED.toString();

    public ExecutionException() {
        super(MSG);
    }

    public ExecutionException(String s) {
        super(String.format("%s : (%s)", MSG, s));
    }

    public int getCode() {
        return CODE;
    }
}