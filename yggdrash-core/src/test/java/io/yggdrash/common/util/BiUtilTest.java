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

package io.yggdrash.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class BiUtilTest {

    @Test
    public void isZero() {
        Assert.assertTrue(BiUtil.isZero(BigInteger.ZERO));
    }

    @Test
    public void isEqual() {
        Assert.assertTrue(BiUtil.isEqual(BigInteger.ONE, BigInteger.ONE));
    }

    @Test
    public void isNotEqual() {
        Assert.assertTrue(BiUtil.isNotEqual(BigInteger.ZERO, BigInteger.ONE));
    }

    @Test
    public void isLessThan() {
        Assert.assertTrue(BiUtil.isLessThan(BigInteger.ZERO, BigInteger.ONE));
    }

    @Test
    public void isMoreThan() {
        Assert.assertTrue(BiUtil.isMoreThan(BigInteger.ONE, BigInteger.ZERO));
    }

    @Test
    public void sum() {
        Assert.assertEquals(11, BiUtil.sum(BigInteger.ONE, BigInteger.TEN).intValue());
    }

    @Test
    public void toBi() {
        Assert.assertEquals(BigInteger.ONE, BiUtil.toBi(1));
    }

    @Test
    public void isPositive() {
        Assert.assertTrue(BiUtil.isPositive(BigInteger.ONE));
    }

    @Test
    public void isCovers() {
        Assert.assertTrue(BiUtil.isCovers(BigInteger.ONE, BigInteger.ZERO));
    }

    @Test
    public void isNotCovers() {
        Assert.assertTrue(BiUtil.isNotCovers(BigInteger.ZERO, BigInteger.ONE));
    }

    @Test
    public void exitLong() {
        Assert.assertTrue(BiUtil.exitLong(new BigInteger(Long.MAX_VALUE + "")));
    }

    @Test
    public void isIn20PercentRange() {
        Assert.assertTrue(BiUtil.isIn20PercentRange(BigInteger.valueOf(5), BigInteger.ONE));
    }

    @Test
    public void max() {
        Assert.assertEquals(BigInteger.ONE, BiUtil.max(BigInteger.ONE, BigInteger.ZERO));
    }

    @Test
    public void addSafely() {
        Assert.assertEquals(10,BiUtil.addSafely(1, 9));
    }
}