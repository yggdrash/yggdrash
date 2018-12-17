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

package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.node.api.dto.AdminDto;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

public class AdminApiImplTest {

    private static long COMMAND_ACTIVE_TIME = 3 * 60 * 1000;

    private static final Logger log = LoggerFactory.getLogger(AdminApiImplTest.class);

    @Test
    public void testJsonMessage() throws IOException {

        String jsonMsg = "{"
                + "\"header\": \"{\\\"timestamp\\\":\\\"00000166818E7D38\\\",\\\"nonce\\\":"
                + "\\\"0000000000000000aabb165899f98a8\\\",\\\"bodyHash\\\":"
                + "\\\"3717ec34f5b0345c3b480d9cd402f0be1111c0e04cb9dbe1da5b933e353a5bba\\\","
                + "\\\"bodyLength\\\":\\\"0000000000000018\\\"}\","
                + "\"signature\": \"1bc1822935fc15c172305d59f134f3f27a305ca97be9926e3cd5e8d4bf5780"
                + "a8332ef34bae62ddb1fe00903b4bf4bfe8c6d5e898cc4f291a3ccf8307d4cc6aec46\","
                + "\"body\": \"[{\\\"method\\\":\\\"nodeHello\\\"}]\""
                + "}";

        log.info(jsonMsg);

        AdminDto command = new ObjectMapper().readValue(jsonMsg, AdminDto.class);

        JsonObject header = JsonUtil.parseJsonObject(command.getHeader());
        log.debug(header.toString());

        JsonArray body = JsonUtil.parseJsonArray(command.getBody());
        log.debug(body.toString());

        String method = body.get(0).getAsJsonObject().get("method").getAsString();

        // body length check
        long bodyLength = ByteUtil.byteArrayToLong(
                Hex.decode(header.get("bodyLength").getAsString()));

        assert command.getBody().length() == bodyLength;

        // body message check
        assert body.get(0).getAsJsonObject().get("method").getAsString().equals("nodeHello");

        // timestamp check (3 min)
        long timestamp = ByteUtil.byteArrayToLong(
                Hex.decode(header.get("timestamp").getAsString()));
        if (timestamp < System.currentTimeMillis() - (COMMAND_ACTIVE_TIME)) {
            log.error("Timestamp is not valid.");
            //assert false;
        }

        // check bodyHash
        if (!header.get("bodyHash").getAsString().equals(
                Hex.toHexString(HashUtil.sha3(body.toString().getBytes())))) {
            log.error("BodyHash is not valid.");
            assert false;
        }

        // verify signature
        String signature = command.getSignature();
        byte[] dataToSign = header.toString().getBytes();
        if (!Wallet.verify(dataToSign, Hex.decode(signature), false)) {
            log.error("Signature is not valid.");
            //assert false;
        }

    }


}
