package io.yggdrash.validator.data;

import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class BlockConTest {

    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
    }

    @Test
    public void constructorTest() {
        Block block = new TestUtils(wallet).sampleBlock();
        List<String> consensusList = new ArrayList<>();
        consensusList.add(Hex.toHexString(wallet.sign(block.getHash())));

        BlockCon blockCon1 = new BlockCon(block.getHeader().getIndex(),
                block.getHeader().getPrevBlockHash(), block, consensusList);
        assertTrue(BlockCon.verify(blockCon1));

        BlockCon blockCon2 = new BlockCon(blockCon1.toBinary());
        assertTrue(BlockCon.verify(blockCon2));
        assertTrue(blockCon1.equals(blockCon2));
    }
}
