package io.yggdrash.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.gateway.dto.BlockDto;
import io.yggdrash.gateway.dto.TransactionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 블록체인에서 발생되는 정보들을 외부 저장소에 수집합니다.
 */
public class BlockChainCollector implements BranchEventListener {
    private static final Logger log = LoggerFactory.getLogger(BlockChainCollector.class);

    private final OutputStore outputStores;
    private final ObjectMapper mapper;

    public BlockChainCollector(OutputStore outputStore) {
        this.outputStores = outputStore;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        String json;
        JsonObject jsonObject = null;

        try {
            json = mapper.writeValueAsString(BlockDto.createBy(block));
            jsonObject = new JsonParser().parse(json).getAsJsonObject();
            jsonObject.addProperty("blockId", block.getHash().toString());
            //TODO: change timestamp spec for elastic search

        } catch (JsonProcessingException e) {
            log.warn("{}", e.getMessage());
        }

        outputStores.put(jsonObject);

        if (block.getBodyCount() > 0) {
            Map<String, JsonObject> transactionMap = new HashMap<>();
            List<TransactionHusk> txs = block.getBody();
            String txHash;
            for (TransactionHusk tx : txs) {
                try {
                    txHash = tx.getHash().toString();
                    json = mapper.writeValueAsString(TransactionDto.createBy(tx));
                    jsonObject = new JsonParser().parse(json).getAsJsonObject();
                    transactionMap.put(txHash, jsonObject);
                } catch (JsonProcessingException e) {
                    log.warn(e.getMessage());
                }
            }

            outputStores.put(block.getHash().toString(), transactionMap);
        }
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {}
}
