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

import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.rlp.RLP;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.yeed.ehtereum.EthTransaction;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class SwapEthTransactionTest {
    private static final Logger log = LoggerFactory.getLogger(SwapEthTransactionTest.class);

    @Test
    public void testEthereumTransactionRlp() {
        // https://etherscan.io/tx/0xa077dd42d4421dca5cb8a7a11aca5b27adaf40be2428f9dc8e42a56abdabeb66
        String ethHexString = "0xf86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethHexString);

        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());


        Assert.assertEquals("Send Address Check",
                "5e032243d507c743b061ef021e2ec7fcc6d3ab89", HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receive Address Check",
                "c3cf7a283a4415ce3c41f5374934612389334780", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "1000000000000000000", ethTransaction.getValue().toString());



        ethHexString = "0xf86e81b68502540be400830493e094735c4b587ae018c4733df6a8ef59711d15f551b48"
                + "80de0b6b3a76400008025a09a5ebf9b742c5a1fb3a6fd931cc419afefdcf5cca371c411a1d5e2b"
                + "55de8dee1a04cbfe5ec19ec5c8fa4a762fa49f17749b0a13a380567da1b75cf80d2faa1a8c9";
        // 0xd21629411073b5ac9e8896f980a2fd066cbea7e6
        // 0x735c4b587ae018c4733df6a8ef59711d15f551b4
        etheSendEncode = HexUtil.hexStringToBytes(ethHexString);
        ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());


        Assert.assertEquals("Send Address Check",
                "d21629411073b5ac9e8896f980a2fd066cbea7e6", HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receive Address Check",
                "735c4b587ae018c4733df6a8ef59711d15f551b4", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "1000000000000000000", ethTransaction.getValue().toString());

        ethHexString = "0xf86c0a8501dcd6500082520894d43276d9b9722a68cc2b5d6ff97a1380d3c9e85e880de0b"
                + "6b3a76400008026a0682ec730a8358498eafdb3fadc9daed305acf59b91e95b234c7cab60285ebb2ea"
                + "038697b3b2bdc325d9e573f36e6bc592f7a94fd9aee09b6424bb13c42d522cf39";
        etheSendEncode = HexUtil.hexStringToBytes(ethHexString);
        ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());


        Assert.assertEquals("Send Address Check",
                "12dab33c2bf841bbd07b173a95d3ffb28608c0e3", HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receive Address Check",
                "d43276d9b9722a68cc2b5d6ff97a1380d3c9e85e", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "1000000000000000000", ethTransaction.getValue().toString());
    }

    @Test
    public void ropstenTransaction() {
        // https://ropsten.etherscan.io/tx/0x5259f3e2269ed193af4400234a4e6e17d0f4302569363e9ef7d180a4af128cbc
        String ropSten = "0xf86f840161d8f3843b9aca0082520894eaf9e2cb3ccebe8c4a301f3d5b643328941f3fb"
                + "2880de0b6b3a7640000801ba0051182b277b67593162ac1293cc5dea264d67b91c235956e5ab489170"
                + "fda363ca06f7a2585ca8bf89a6b31022a19852894822bfe63ca9ed26e21a0da708ef8a0a5";

        byte[] ethRawEncode = HexUtil.hexStringToBytes(ropSten);

        EthTransaction ethTransaction = new EthTransaction(ethRawEncode);

        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());

        Assert.assertEquals("Send Address Check",
                "81b7e08f65bdf5648606c89998a9cc8164397647", HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receive Address Check",
                "eaf9e2cb3ccebe8c4a301f3d5b643328941f3fb2", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "1000000000000000000", ethTransaction.getValue().toString());

        String ropsten2 = "0xf86c808503b9aca00082520894101167aaf090581b91c08480f6e559acdd9a3ddd880de0b6b3a7640000802aa0"
                + "480412d6afba1296abcf33e529f9f91e50900597eb69387f52da909d48bf5593a02afa99f81ebe96958ccaee9f2d4ba03aaf"
                + "9de1cda6bcb98ee6e4418110ea3d7f";
        ethRawEncode = HexUtil.hexStringToBytes(ropsten2);

        ethTransaction = new EthTransaction(ethRawEncode);
        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());
    }

    @Test
    public void kovanTransaction() {
        // https://kovan.etherscan.io/tx/0xcca9e56639aa3a5da3dfe715bad74f160bbf1004402da0da87e3cd8f833c409b
        String kovan = "0xf86f829e3f85037baef359830186a094b116c9bf208f0941ed2045c1b8d8e0c14854421c8"
                + "829a2241af62c0000801ca0c9cfc6fb986f73b8273480398da62b8353da8ca53696b36f3342572a486"
                + "ea2e0a07ad9e6b34f0d1cd06dc7802e894e170529c70e567a602f4c8a95762bdb51826c";

        byte[] ethRawEncode = HexUtil.hexStringToBytes(kovan);

        EthTransaction ethTransaction = new EthTransaction(ethRawEncode);

        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());

        Assert.assertEquals("Send Address Check",
                "003bbce1eac59b406dd0e143e856542df3659075", HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receive Address Check",
                "b116c9bf208f0941ed2045c1b8d8e0c14854421c", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "3000000000000000000", ethTransaction.getValue().toString());

        // https://kovan.etherscan.io/tx/0x5c9d9e621794d02be33598a4568a8daacb954477e685b5b32ff9ff5dac9173a4
        kovan = "0xf86b56843b9aca0082520894459bedae42e98109136a1463039c436dba53cede880de0b6b3a76400"
                + "008078a048402bfb17a5296973ba8174a6f8aba5d91d2a463de78925d2df3431574cf9dda02ad1f070"
                + "1a04f8d6f58b3ba89481c9febd130c29282737bde20b5b41ec11e314";
        ethRawEncode = HexUtil.hexStringToBytes(kovan);
        ethTransaction = new EthTransaction(ethRawEncode);

        Assert.assertEquals("Send Address Check",
                "bd711b985a49c3f95d62250d8d3d080a5151a6a5", HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receive Address Check",
                "459bedae42e98109136a1463039c436dba53cede", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "1000000000000000000", ethTransaction.getValue().toString());

    }

    @Test
    @Ignore
    public void goerliTransaction() {
        // Goerl TEST NET Not support
        String goerli = "0xf86c821717843b9aca0082753094f689d779d9108faa0ba447399fb247768792327887b"
                + "1a2bc2ec50000001ca0ee5533baf4ed23e9204304bb692cb10874592bc90688033848c9d65c257"
                + "b6f62a01652ef0de0c289f4f5a554135018d0adfecfda2f65b5722d7c80d839180484dc";

        byte[] ethRawEncode = HexUtil.hexStringToBytes(goerli);

        EthTransaction ethTransaction = new EthTransaction(ethRawEncode);

        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());

        Assert.assertEquals("Send Address Check",
                "8ced5ad0d8da4ec211c17355ed3dbfec4cf0e5b9", HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receive Address Check",
                "f689d779d9108faa0ba447399fb2477687923278", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "50000000000000000", ethTransaction.getValue().toString());
    }

    @Test
    public void klytnTransaction() {
        // 2019 04 18 TEST COMPLATE
        // KLYTH TEST NET
        String klyth = "0xf86e808505d21dba008261a8945b71a37bb39b33dd40d267a989ac3e448735a7c2880de0"
                + "b6b3a7640000808207f5a00ce36efc94d3335df54b73ed8224d22600016a4d7d686cec03901692f"
                + "80c21b2a056f6dc38fa550e00f025166c7910497ec246a5b29f74f27033dfb39d4426eceb";
        byte[] ethRawEncode = HexUtil.hexStringToBytes(klyth);

        EthTransaction ethTransaction = new EthTransaction(ethRawEncode);

        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());
        // https://baobab.klaytnscope.com/tx/0xdf2f5c43fe9b613b4692f089774d2d9ec4bc8df02fa73d8aa4196279898f4457

        // 0x17b396E80db97258885FFbC0513eF1bbF1F1F1d0
        // 0x5b71a37bb39b33dd40d267a989ac3e448735a7c2
        // 1 KLAY = 1000000000000000000
        Assert.assertEquals("sender", "17b396e80db97258885ffbc0513ef1bbf1f1f1d0",
                HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receiver", "5b71a37bb39b33dd40d267a989ac3e448735a7c2",
                HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "1000000000000000000", ethTransaction.getValue().toString());



        klyth = "0xf86e018505d21dba00826d60947eca82d239c3b1513d6368e2ff21763e469f129888016345785d"
                + "8a0000808207f5a07379dc1afe1e467ccc966056a6232a4166d28032f683518d49a6290e2740dc"
                + "a6a01d4d215150363958824f106ee97a42919faee68d3f87a3cb3473dbec0a9239ca";
        // https://baobab.klaytnscope.com/tx/0x3dd862fad8cb4d33bbf284cf4b6481e5e5712f432f6c2f2a873fe306d249401d
        // 0x17b396E80db97258885FFbC0513eF1bbF1F1F1d0
        // 0x7eca82d239c3b1513d6368e2ff21763e469f1298
        // 0.1 KLAY = 100000000000000000
        ethRawEncode = HexUtil.hexStringToBytes(klyth);

        ethTransaction = new EthTransaction(ethRawEncode);

        log.debug("txHash {} ", HexUtil.toHexString(ethTransaction.getTxHash()));

        log.debug("Chain ID {} ", ethTransaction.getChainId());
        log.debug("Sender {}", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("value {}", ethTransaction.getValue());
        log.debug("data {}", ethTransaction.getData());

        Assert.assertEquals("sender", "17b396e80db97258885ffbc0513ef1bbf1f1f1d0",
                HexUtil.toHexString(ethTransaction.getSendAddress()));
        Assert.assertEquals("Receiver", "7eca82d239c3b1513d6368e2ff21763e469f1298",
                HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        Assert.assertEquals("Send Value", "100000000000000000", ethTransaction.getValue().toString());

    }

    @Test
    public void rawTransactionFromApiCallResult() {
        InputStream file = getClass().getResourceAsStream("/mainnet-hash.json");
        Reader json = new InputStreamReader(file, FileUtil.DEFAULT_CHARSET);
        JsonObject obj = JsonUtil.parseJsonObject(json);

        log.debug(obj.toString());

        byte[] nonce = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("nonce").getAsString()));
        byte[] gasPrice = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("gasPrice").getAsString()));
        byte[] gasLimit = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("gas").getAsString()));
        byte[] receiveAddress = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("to").getAsString()));
        byte[] value = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("value").getAsString()));
        byte[] data = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("input").getAsString()));
        byte[] v = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("v").getAsString()));
        byte[] r = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("r").getAsString()));
        byte[] s = RLP.encodeElement(HexUtil.hexStringToBytes(obj.get("s").getAsString()));

        byte[] rawTransaction = RLP.encodeList(nonce, gasPrice, gasLimit, receiveAddress,
                value, data, v, r, s);

        String ethHexString = "f86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";
        byte[] ethHex = HexUtil.hexStringToBytes(ethHexString);

        Assert.assertArrayEquals("Json rpc Api to rawTransaction", ethHex, rawTransaction);



    }

}
