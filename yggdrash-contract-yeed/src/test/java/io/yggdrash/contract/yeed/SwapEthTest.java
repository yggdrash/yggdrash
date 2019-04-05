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

package io.yggdrash.contract.yeed;

import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.contract.yeed.ehtereum.Eth;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.security.SignatureException;

public class SwapEthTest {
    private static final Logger log = LoggerFactory.getLogger(SwapEthTest.class);

    @Test
    public void testEthereumTransactionRLP() {
        String ethHexString = "0xf86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethHexString);

        Eth eth = new Eth(etheSendEncode);

        log.debug("Chain ID {} ", eth.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(eth.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(eth.getReceiveAddress()));
        log.debug("value {}", new BigInteger(eth.getValue()));
        log.debug("data {}", eth.getData());
    }

}
