/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.common.contract.standard;

import com.google.gson.JsonObject;
import io.yggdrash.contract.core.Receipt;

import java.math.BigInteger;

public interface CoinStandard {

    // Query
    BigInteger totalSupply();

    BigInteger balanceOf(JsonObject params);

    BigInteger allowance(JsonObject params);


    // Transaction
    Receipt transfer(JsonObject params);

    Receipt approve(JsonObject params);

    Receipt transferFrom(JsonObject params);

}