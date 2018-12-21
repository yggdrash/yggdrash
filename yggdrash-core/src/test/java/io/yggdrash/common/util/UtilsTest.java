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

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Locale;

public class UtilsTest {

    @Before
    public void setUp() {
        Locale.setDefault(Locale.KOREA);
    }

    @Test
    public void longToTimePeriod() {
        assert Utils.longToTimePeriod(1).equals("1ms");
    }

    @Test
    public void getValueShortString() {
        assert Utils.getValueShortString(BigInteger.ONE).equals("1·(10^0)");
    }

    @Test
    public void getRandom() {
        assert Utils.getRandom() != null;
    }

    @Test
    public void getJavaVersion() {
        assert Utils.getJavaVersion() > 1;
    }

    @Test
    public void getNodeIdShort() {
        assert Utils.getNodeIdShort("abcdefghijkl").equals("abcdefgh");
        assert Utils.getNodeIdShort(null).equals("<null>");
    }

    @Test
    public void toUnixTime() {
        assert Utils.toUnixTime(1000) == 1;
    }

    @Test
    public void fromUnixTime() {
        assert Utils.fromUnixTime(1) == 1000;
    }

    @Test
    public void mergeArrays() {
        assert Utils.mergeArrays(new String[] {"a", "b"}).length == 2;
    }

    @Test
    public void align() {
        assert Utils.align("test", 'c', 1, true).equals("test");
        assert Utils.align("test", '0', 6, true).equals("00test");
    }

    @Test
    public void repeat() {
        assert Utils.repeat("a", 2).equals("aa");
    }

    @Test(expected = RuntimeException.class)
    public void showErrorAndExit() {
        Utils.showErrorAndExit("test");
    }

    @Test()
    public void showWarn() {
        Utils.showWarn("test");
    }

    @Test
    public void sizeToStr() {
        assert Utils.sizeToStr(1).equals("1b");
    }

    @Test
    public void sleep() {
        Utils.sleep(10);
    }
}