package io.yggdrash.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.util.Timestamps;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.gateway.dto.BlockDto;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.store.es.EsClient;
import org.elasticsearch.common.util.set.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 블록체인에서 발생되는 정보들을 외부 저장소에 수집합니다.
 */
@Component
@DependsOn("yggdrash")
@ConditionalOnProperty("es.host")
public class BlockChainCollector implements BranchEventListener {
    private static final Logger log = LoggerFactory.getLogger(BlockChainCollector.class);

    @Value("${es.host")
    private String esHost;
    @Value("${es.transport")
    private String esTransport;
    @Value("${event.store:#{null}}")
    private String[] eventStore;

    private final OutputStore outputStores;
    private final ObjectMapper mapper;

    public BlockChainCollector(BranchGroup branchGroup) {
        this.outputStores = EsClient.newInstance("localhost", 9300, Sets.newHashSet());
        for (BlockChain bc : branchGroup.getAllBranch()) {
            bc.addListener(this);
        }
        this.mapper = new ObjectMapper();
    }

    @Override
    public void chainedBlock(ConsensusBlock block) {
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

        if (block.getBlock().getBody().getLength() > 0) {
            Map<String, JsonObject> transactionMap = new HashMap<>();
            List<Transaction> txs = block.getBody().getTransactionList();
            String txHash;
            for (Transaction tx : txs) {
                try {
                    txHash = tx.getHash().toString();
                    json = mapper.writeValueAsString(TransactionDto.createBy(tx));
                    jsonObject = new JsonParser().parse(json).getAsJsonObject();
                    long t = jsonObject.get("timestamp").getAsLong();
                    jsonObject.addProperty("timestamp",
                            Timestamps.toString(Timestamps.fromMillis(t)));
                    transactionMap.put(txHash, jsonObject);
                } catch (JsonProcessingException e) {
                    log.warn(e.getMessage());
                }
            }

            outputStores.put(block.getHash().toString(), transactionMap);
        }
    }

    @Override
    public void receivedTransaction(Transaction tx) {

    }
}
