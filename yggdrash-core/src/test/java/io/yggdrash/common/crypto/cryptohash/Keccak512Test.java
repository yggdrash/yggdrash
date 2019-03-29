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

package io.yggdrash.common.crypto.cryptohash;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Keccak512Test {

    private Keccak512 keccak512;

    @Before
    public void setUp() {
        keccak512 = new Keccak512();
    }

    @Test
    public void copy() {
        Digest digest = keccak512.copy();
        Assert.assertNotNull(digest);
    }

    @Test
    public void engineGetDigestLength() {
        Assert.assertEquals(64, keccak512.engineGetDigestLength());
    }

    @Test
    public void engineDigest() {
        Assert.assertNull(keccak512.engineDigest());
    }

    @Test
    public void engineUpdate() {
        keccak512.engineUpdate((byte) 'b');
        keccak512.engineUpdate("byte".getBytes(), 0, 4);
    }

}