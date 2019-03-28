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
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class UtilsTest {

    @Before
    public void setUp() {
        Locale.setDefault(Locale.KOREA);
    }

    @Test
    public void longToTimePeriod() {
        Assert.assertEquals("1ms", Utils.longToTimePeriod(1));
    }

    @Test
    public void getRandom() {
        Assert.assertNotNull(Utils.getRandom());
    }

    @Test
    public void getJavaVersion() {
        Assert.assertTrue(Utils.getJavaVersion() > 1);
    }

    @Test
    public void getNodeIdShort() {
        Assert.assertEquals("abcdefgh", Utils.getNodeIdShort("abcdefghijkl"));
        Assert.assertEquals("<null>", Utils.getNodeIdShort(null));
    }

    @Test
    public void toUnixTime() {
        Assert.assertEquals(1, Utils.toUnixTime(1000));
    }

    @Test
    public void fromUnixTime() {
        Assert.assertEquals(1000, Utils.fromUnixTime(1));
    }

    @Test
    public void mergeArrays() {
        Assert.assertEquals(2, Utils.mergeArrays(new String[] {"a", "b"}).length);
    }

    @Test
    public void align() {
        Assert.assertEquals("test", Utils.align("test", 'c', 1, true));
        Assert.assertEquals("00test", Utils.align("test", '0', 6, true));
    }

    @Test
    public void repeat() {
        Assert.assertEquals("aa", Utils.repeat("a", 2));
    }

    @Test(expected = RuntimeException.class)
    public void showErrorAndExit() {
        Utils.showErrorAndExit("test");
    }

    @Test
    public void showWarn() {
        Utils.showWarn("test");
    }

    @Test
    public void sizeToStr() {
        Assert.assertEquals("1b", Utils.sizeToStr(1));
    }

}