package io.yggdrash.core.net;

import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BranchId;
import java.io.IOException;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class BestBlockTest {
    private final BranchId branchId = TestConstants.yggdrash();

    public BestBlockTest() throws IOException {
    }

    @Test
    public void testBestBlock() {
        BestBlock bb = BestBlock.of(branchId, 0);
        assert bb.getBranchId().equals(branchId);
        assert bb.getIndex() == 0;
    }

    @Test
    public void shouldBeUniqueSameBranchInTheList() {
        // arrange
        BestBlock bb1 = BestBlock.of(branchId, 0);
        BestBlock bb2 = BestBlock.of(branchId, 2);
        assert bb1.hashCode() == bb2.hashCode();
        assert bb1.equals(bb2);

        // act
        Set<BestBlock> bestBlocks = new HashSet<>();
        bestBlocks.add(bb1);
        bestBlocks.add(bb2);

        // assert
        assert bestBlocks.size() == 1;

        // act again
        bestBlocks.add(BestBlock.of(TestConstants.YEED, 0));

        // assert again
        assert bestBlocks.size() == 2;

    }
}