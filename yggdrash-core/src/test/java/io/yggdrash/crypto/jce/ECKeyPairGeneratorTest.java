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

package io.yggdrash.crypto.jce;

import org.junit.Test;
import sun.security.provider.Sun;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ECKeyPairGeneratorTest {

    @Test
    public void generateKeyPair() {
        ECKeyPairGenerator.generateKeyPair();
    }

    @Test(expected = NoSuchAlgorithmException.class)
    public void getInstanceByProviderName() throws Exception {
        ECKeyPairGenerator.getInstance("test", SecureRandom.getInstance("EC"));
    }

    @Test(expected = NoSuchAlgorithmException.class)
    public void getInstanceByProvier() throws NoSuchAlgorithmException {
        ECKeyPairGenerator.getInstance(new Sun(), SecureRandom.getInstance("EC"));
    }
}