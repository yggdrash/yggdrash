package io.yggdrash.core.blockchain.pbft;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.pbft.PbftBlockKeyStore;
import io.yggdrash.core.store.pbft.PbftBlockStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PbftBlockChain {

    private final Peer owner;

    private final BlockChain coreBlockChain;
    private final PbftBlock genesisBlock;
    private final PbftBlockKeyStore blockKeyStore;
    private final PbftBlockStore blockStore;
    private final TransactionStore transactionStore;

    private PbftBlock lastConfirmedBlock;

    private final Map<String, PbftMessage> unConfirmedMsgMap = new ConcurrentHashMap<>();

    public PbftBlockChain(Peer owner, BlockChain blockChain, PbftBlockKeyStore blockKeyStore,
                          PbftBlockStore blockStore, TransactionStore transactionStore) {
        this.owner = owner;
        this.coreBlockChain = blockChain;
        this.genesisBlock = new PbftBlock(blockChain.getBlockByIndex(0).getCoreBlock(), null);
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

    public Peer getOwner() {
        return owner;
    }

    public JsonObject getConsensus() {
        return coreBlockChain.getBranch().getConsensus();
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

    public void addBlockInternal(PbftBlock pbftBlock) {
        coreBlockChain.addBlock(new BlockHusk(pbftBlock.getBlock()), true);
    }
}
