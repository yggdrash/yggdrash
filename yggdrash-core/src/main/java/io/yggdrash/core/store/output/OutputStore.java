package io.yggdrash.core.store.output;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Transaction;

import java.util.List;

public interface OutputStore {
    String put(BlockHusk nextBlock);

    List<String> put(long blockNo, List<Transaction> transactions);
}
