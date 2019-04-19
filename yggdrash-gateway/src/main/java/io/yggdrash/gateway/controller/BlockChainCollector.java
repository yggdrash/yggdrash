package io.yggdrash.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.consensus.Block;
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
public class BlockChainCollector {
    private static final Logger log = LoggerFactory.getLogger(BlockChainCollector.class);

    private final OutputStore outputStores;
    private final ObjectMapper mapper;

    public BlockChainCollector(OutputStore outputStore) {
        this.outputStores = outputStore;
        this.mapper = new ObjectMapper();
    }

    public void block(Block block) {
        String json;
        JsonObject jsonObject = null;

        try {
            json = mapper.writeValueAsString(BlockDto.createBy(block));
            jsonObject = new JsonParser().parse(json).getAsJsonObject();
            jsonObject.addProperty("blockId", block.getHash().toString());

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

    public void transaction(TransactionHusk tx) { }
}
