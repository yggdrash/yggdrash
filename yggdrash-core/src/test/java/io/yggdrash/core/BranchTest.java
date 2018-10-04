/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core;

import io.yggdrash.TestUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class BranchTest {

    @Test
    public void branchTest() {
        Branch stemBranch = Branch.of(BranchId.STEM, Branch.STEM, TestUtils.OWNER);
        assert stemBranch.getBranchId().equals(BranchId.stem());
        assert !stemBranch.isYeed();

        BranchId yeed = BranchId.of(Hex.decode(BranchId.YEED));
        Branch yeedBranch = Branch.of(yeed.toString(), Branch.YEED, TestUtils.OWNER);
        assert yeedBranch.isYeed();
    }
}