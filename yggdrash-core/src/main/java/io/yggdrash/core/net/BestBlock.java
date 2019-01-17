/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BranchId;

import java.util.Arrays;

public class BestBlock {
    private BranchId branchId;
    private long index;

    public BranchId getBranchId() {
        return branchId;
    }

    public void setBranchId(BranchId branchId) {
        this.branchId = branchId;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(branchId.getBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BestBlock bestBlock = (BestBlock) o;
        return branchId.equals(bestBlock.getBranchId());
    }

    public static BestBlock of(BranchId branchId, long index) {
        BestBlock bestBlock = new BestBlock();
        bestBlock.setBranchId(branchId);
        bestBlock.setIndex(index);
        return bestBlock;
    }
}
