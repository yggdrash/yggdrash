package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.exception.InternalErrorException;
import io.yggdrash.node.exception.NonExistObjectException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AutoJsonRpcServiceImpl
public class BlockApiImpl implements BlockApi {

    private final NodeManager nodeManager;

    public BlockApiImpl(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public int blockNumber() {
        try {
            return 0;
        } catch (Exception exception) {
            throw new InternalErrorException();
        }
    }

    @Override
    public Block getBlockByHash(String address, Boolean bool) {
        try {
            //todo: getBlockByNumber
            return retBlockMock();
        } catch (Exception exception) {
            System.out.println("\n\nException :: getBlockHashImp");
            exception.printStackTrace();
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public Block getBlockByNumber(String hashOfBlock, Boolean bool) {
        try {
            //todo: getBlockByNumber
            return retBlockMock();
        } catch (Exception exception) {
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public int newBlockFilter() {
        try {
            return 0;
        } catch (Exception exception) {
            throw new InternalErrorException();
        }
    }

    public Block retBlockMock() {
        // Create transactions
        JsonObject txObj1 = new JsonObject();

        txObj1.addProperty("operator", "transfer");
        txObj1.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj1.addProperty("value", 30);

        JsonObject txObj2 = new JsonObject();
        txObj2.addProperty("operator", "transfer");
        txObj2.addProperty("to", "0xdB44902E6cE92fa71Bbf06312630Cb39c5bE756C");
        txObj2.addProperty("value", 40);

        JsonObject txObj3 = new JsonObject();
        txObj3.addProperty("operator", "transfer");
        txObj3.addProperty("to", "0xA0A2fceBF3f3cc182eCfcbB65042Af0fB43dd864");
        txObj3.addProperty("value", 50);

        Transaction tx1 = new Transaction(txObj1);
        Transaction tx2 = new Transaction(txObj2);
        Transaction tx3 = new Transaction(txObj3);

        List<Transaction> txList = new ArrayList<>();
        txList.add(nodeManager.signByNode(tx1));
        txList.add(nodeManager.signByNode(tx2));
        txList.add(nodeManager.signByNode(tx3));

        // Create a blockBody
        BlockBody blockBody = new BlockBody(txList);

        // Create a blockHeader
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(null)
                .blockBody(blockBody).build(nodeManager.getWallet());

        // Return a created block
        return new Block(blockHeader, blockBody);
    }
}
