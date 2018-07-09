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

package io.yggdrash.util;

import org.junit.Test;

import java.math.BigInteger;

public class BiUtilTest {

    @Test
    public void isZero() {
        assert BiUtil.isZero(BigInteger.ZERO);
    }

    @Test
    public void isEqual() {
        assert BiUtil.isEqual(BigInteger.ONE, BigInteger.ONE);
    }

    @Test
    public void isNotEqual() {
        assert BiUtil.isNotEqual(BigInteger.ZERO, BigInteger.ONE);
    }

    @Test
    public void isLessThan() {
        assert BiUtil.isLessThan(BigInteger.ZERO, BigInteger.ONE);
    }

    @Test
    public void isMoreThan() {
        assert BiUtil.isMoreThan(BigInteger.ONE, BigInteger.ZERO);
    }

    @Test
    public void sum() {
        assert BiUtil.sum(BigInteger.ONE, BigInteger.TEN).intValue() == 11;
    }

    @Test
    public void toBi() {
        assert BiUtil.toBi(1).equals(BigInteger.ONE);
    }

    @Test
    public void isPositive() {
        assert BiUtil.isPositive(BigInteger.ONE);
    }

    @Test
    public void isCovers() {
        assert BiUtil.isCovers(BigInteger.ONE, BigInteger.ZERO);
    }

    @Test
    public void isNotCovers() {
        assert BiUtil.isNotCovers(BigInteger.ZERO, BigInteger.ONE);
    }

    @Test
    public void exitLong() {
        assert BiUtil.exitLong(new BigInteger(Long.MAX_VALUE + ""));
    }

    @Test
    public void isIn20PercentRange() {
        assert BiUtil.isIn20PercentRange(BigInteger.valueOf(5), BigInteger.ONE);
    }

    @Test
    public void max() {
        assert BiUtil.max(BigInteger.ONE, BigInteger.ZERO).equals(BigInteger.ONE);
    }

    @Test
    public void addSafely() {
        assert BiUtil.addSafely(1, 9) == 10;
    }
}