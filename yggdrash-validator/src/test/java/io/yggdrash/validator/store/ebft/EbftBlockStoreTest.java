package io.yggdrash.validator.store.ebft;

import com.google.protobuf.ByteString;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.TestUtils;
import io.yggdrash.validator.data.ebft.EbftBlock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EbftBlockStoreTest {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockStoreTest.class);

    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet(new DefaultConfig(), "Password1234!");
    }

    @Test
    public void ebftBlockStoreTest() {
        LevelDbDataSource ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "block-con-store-test");
        EbftBlockStore ebftBlockStore = new EbftBlockStore(ds);
        Block block = new TestUtils(wallet).sampleBlock();
        List<ByteString> consensusList = new ArrayList<>();
        consensusList.add(wallet.signByteString(block.getHash().getBytes(), true));

        EbftBlock ebftBlock = new EbftBlock(block, consensusList);

        ebftBlockStore.put(ebftBlock.getHash(), ebftBlock);
        EbftBlock foundEbftBlock = ebftBlockStore.get(ebftBlock.getHash());

        assert (ebftBlock.equals(foundEbftBlock));
        assert (ebftBlockStore.contains(ebftBlock.getHash()));

        log.debug("size: {}", ebftBlockStore.size());
        assertEquals(1, ebftBlockStore.size());

        StoreTestUtils.clearTestDb();
    }

}
