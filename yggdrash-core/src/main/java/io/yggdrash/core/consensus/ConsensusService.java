package io.yggdrash.core.consensus;

import java.util.concurrent.locks.ReentrantLock;

public interface ConsensusService<T, V> extends Runnable {

    ConsensusBlockChain<T, V> getBlockChain();

    ReentrantLock getLock();
}
