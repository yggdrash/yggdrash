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

import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.contract.yeed.ehtereum.EthTokenTransaction;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.pqc.math.linearalgebra.ByteUtils;
import java.math.BigInteger;
import java.util.Arrays;

public class SwapEthTransactionTokenTest {
    private static final Logger log = LoggerFactory.getLogger(SwapEthTransactionTokenTest.class);

    @Test
    public void testEthereumTokenTransactionRlp() {
        String ethHexString = "0xf8a97385098bca5a0082ce0394ca2796f9f61dc7b238aab043971e49c6164df37"
                + "580b844a9059cbb000000000000000000000000e861e4edfddcb969a7cc485cf155fa5a415c94f30"
                + "000000000000000000000000000000000000000000068d200f701997760000025a0ea93891a9273b6c"
                + "08e5e507275a53968d18bc0eb5e2e00383ba10db4f8590c40a00c55b0db0d88a574b07d044eeb2f541"
                + "b95156347e5ceda29975b351641ffe029";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethHexString);

        EthTokenTransaction token = new EthTokenTransaction(etheSendEncode);
        log.debug("transaction hash {}",HexUtil.toHexString(token.getTxHash()));

        log.debug("EthTransaction Data {} ", HexUtil.toHexString(token.getData()));

        log.debug(HexUtil.toHexString(token.getMethod()));
        log.debug("{} param", token.getParam().length);
        // Token Receive Address
        log.debug(HexUtil.toHexString(Arrays.copyOfRange(token.getParam()[0],12,32)));

        log.debug(HexUtil.toHexString(token.getParam()[1]));
        BigInteger tokenAmount = new BigInteger(token.getParam()[1]);
        log.debug("send Token {}", tokenAmount);
        log.debug("calculate Token {} ", tokenAmount.divide(new BigInteger("1000000000000000000")));
        // 495,000

        log.debug("Address {} ", HexUtil.toHexString(token.getReceiveAddress()));
        assert ByteUtils.equals(token.getReceiveAddress(),
                HexUtil.hexStringToBytes("0xca2796f9f61dc7b238aab043971e49c6164df375"));


        String ethTokenRawTx = "0xf8aa138501d0ea8e00830249f094ad22f63404f7305e4713ccbd4f296f3477051"
                + "3f480b844a9059cbb000000000000000000000000940c3a376fe6e05c46e88433ed4601d87d0adb470"
                + "0000000000000000000000000000000000000000000000000000002540be40026a07e15c6842280907"
                + "ea5dafbefe81b322b38eb66ea4a79fcc3581ebceff7a06106a00a5b7b5a22297fc3df87b641b59b766"
                + "29ed865459e83f993b9770c1bf675dc9a";
        byte[] ethSendTokenEncode = HexUtil.hexStringToBytes(ethTokenRawTx);
        EthTokenTransaction token2 = new EthTokenTransaction(ethSendTokenEncode);
        log.debug("Token Method {}", HexUtil.toHexString(token2.getMethod()));


        BigInteger token2Amount = new BigInteger(token2.getParam()[1]);
        log.debug("send Token {}", token2Amount);


        String burnTokenRawTx = "0xf8aa808504e3b2920083011db094b5a5f22694352c15b00323844ad545abb2b1"
                + "102880b844a9059cbb0000000000000000000000000000000000000000000000000000000000000000"
                + "000000000000000000000000000000000000000000000025f273933db57000001ca040809cb1a31738"
                + "2b6a9c48fb349c2274fdaabf9fc3ec294a7bdacb87b57f4ea4a066e68feab9307fb7dc454d02a78323"
                + "dfd302fe9b49fab52d21839389375ffbee";

        EthTokenTransaction token3 = new EthTokenTransaction(HexUtil.hexStringToBytes(burnTokenRawTx));
        log.debug("Tx : {}", HexUtil.toHexString(token3.getTxHash()));
        log.debug("Receive : {}", HexUtil.toHexString(token3.getParam()[0]));
        assert ByteUtils.equals(token3.getParam()[0], new byte[32]);

        log.debug("Amount : {}", new BigInteger(token3.getParam()[1]));
    }

}
