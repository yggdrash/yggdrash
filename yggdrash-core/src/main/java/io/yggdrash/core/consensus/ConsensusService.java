package io.yggdrash.core.consensus;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public interface ConsensusService<T1, T2, T3> extends Runnable {

    ConsensusBlockChain<T1, T2> getBlockChain();

    ReentrantLock getLock();

    Map<String, T3> getTotalValidatorMap();
}
