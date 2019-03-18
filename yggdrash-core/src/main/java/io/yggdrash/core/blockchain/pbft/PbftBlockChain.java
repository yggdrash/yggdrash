package io.yggdrash.core.blockchain.pbft;

import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.pbft.PbftBlockKeyStore;
import io.yggdrash.core.store.pbft.PbftBlockStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PbftBlockChain {

    private final Peer owner;

    private final PbftBlock genesisBlock;
    private final PbftBlockKeyStore blockKeyStore;
    private final PbftBlockStore blockStore;
    private final TransactionStore transactionStore;

    private PbftBlock lastConfirmedBlock;

    private final Map<String, PbftMessage> unConfirmedMsgMap = new ConcurrentHashMap<>();

    public PbftBlockChain(Peer owner, PbftBlock genesisBlock, PbftBlockKeyStore blockKeyStore,
                          PbftBlockStore blockStore, TransactionStore transactionStore) {
        this.owner = owner;
        this.genesisBlock = genesisBlock;
        this.blockKeyStore = blockKeyStore;
        this.blockStore = blockStore;
        this.transactionStore = transactionStore;

        if (blockKeyStore.size() == 0) {
            blockKeyStore.put(0L, this.genesisBlock.getHash());
            blockStore.put(this.genesisBlock.getHash(), this.genesisBlock);
            this.lastConfirmedBlock = genesisBlock;
        } else {
            long bestBlock = blockKeyStore.size();
            this.lastConfirmedBlock = blockStore.get(blockKeyStore.get(bestBlock));
        }
    }

    public byte[] getChain() {
        return genesisBlock.getBlock().getChain();
    }

    public String getHost() {
        return owner.getHost();
    }

    public int getPort() {
        return owner.getPort();
    }

    public PbftBlockKeyStore getBlockKeyStore() {
        return blockKeyStore;
    }

    public PbftBlockStore getBlockStore() {
        return blockStore;
    }

    public PbftBlock getGenesisBlock() {
        return genesisBlock;
    }

    public void setLastConfirmedBlock(PbftBlock lastConfirmedBlock) {
        this.lastConfirmedBlock = lastConfirmedBlock;
    }

    public PbftBlock getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    public Map<String, PbftMessage> getUnConfirmedMsgMap() {
        return unConfirmedMsgMap;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }
}
