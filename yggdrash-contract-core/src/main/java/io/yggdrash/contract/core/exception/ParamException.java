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

public class ParamException extends Exception {

    public static final int CODE = ApplicationError.INVALID_PARAMS.toValue();
    private static final String MSG = ApplicationError.INVALID_PARAMS.toString();

    public ParamException() {
        super(MSG);
    }

    public int getCode() {
        return CODE;
    }
}