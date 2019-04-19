/*
 * Copyright 2018 Akashic Foundation
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

package io.yggdrash.common.crypto;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class ECIESCoderTest {

    private static final Logger log = LoggerFactory.getLogger(ECIESCoderTest.class);

    @Test // decrypt cpp data
    public void test1() {
        BigInteger privKey = new BigInteger("5e173f6ac3c669587538e7727cf19b782a4f2fda07c1eaa662c593e5e85e3051", 16);
        byte[] cipher = Hex.decode("049934a7b2d7f9af8fd9db941d9da281ac9381b5740e1f64f7092f3588d4f87f5ce55191a6653e5e80c1c5dd538169aa123e70dc6ffc5af1827e546c0e958e42dad355bcc1fcb9cdf2cf47ff524d2ad98cbf275e661bf4cf00960e74b5956b799771334f426df007350b46049adb21a6e78ab1408d5e6ccde6fb5e69f0f4c92bb9c725c02f99fa72b9cdc8dd53cff089e0e73317f61cc5abf6152513cb7d833f09d2851603919bf0fbe44d79a09245c6e8338eb502083dc84b846f2fee1cc310d2cc8b1b9334728f97220bb799376233e113");

        byte[] payload = new byte[0];
        try {
            payload = ECIESCoder.decrypt(privKey, cipher);
        } catch (Throwable e) {
            log.error(e.getMessage());
        }

        Assert.assertEquals("802b052f8b066640bba94a4fc39d63815c377fced6fcb84d27f791c9921ddf3e9bf0108e298f490812847109cbd778fae393e80323fd643209841a3b7f110397f37ec61d84cea03dcc5e8385db93248584e8af4b4d1c832d8c7453c0089687a700",
                Hex.toHexString(payload));
    }

    @Test  // encrypt decrypt round trip
    public void test2() {

        BigInteger privKey = new BigInteger("5e173f6ac3c669587538e7727cf19b782a4f2fda07c1eaa662c593e5e85e3051", 16);

        byte[] payload = Hex.decode("1122334455");

        ECKey ecKey = ECKey.fromPrivate(privKey);
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();

        byte[] cipher = new byte[0];
        try {
            cipher = ECIESCoder.encrypt(pubKeyPoint, payload);
        } catch (Throwable e) {
            log.error(e.getMessage());
        }

        log.debug(Hex.toHexString(cipher));

        byte[] decrypted_payload = new byte[0];
        try {
            decrypted_payload = ECIESCoder.decrypt(privKey, cipher);
        } catch (Throwable e) {
            log.error(e.getMessage());
        }

        Assert.assertEquals("1122334455", Hex.toHexString(decrypted_payload));
    }

}
