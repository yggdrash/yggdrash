package io.yggdrash.validator.data;

import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EbftBlockTest {

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

        EbftBlock ebftBlock1 = new EbftBlock(block.getHeader().getIndex(),
                block.getHeader().getPrevBlockHash(), block, consensusList);
        assertTrue(EbftBlock.verify(ebftBlock1));

        EbftBlock ebftBlock2 = new EbftBlock(ebftBlock1.toBinary());
        assertTrue(EbftBlock.verify(ebftBlock2));
        assertTrue(ebftBlock1.equals(ebftBlock2));
    }
}
