package io.yggdrash.core.store.output.es;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.store.output.OutputStore;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EsClient implements OutputStore {
    private static final Logger log = LoggerFactory.getLogger(EsClient.class);

    private final String blockIndex = "block";
    private final String txIndex = "tx";

    public TransportClient client;
    private Set<String> eventSet;

    private EsClient(TransportClient client, Set<String> eventSet) {
        this.client = client;
        this.eventSet = eventSet;
    }

    public static EsClient newInstance(String host, int port, String[] events) {
        Settings settings = Settings.builder()
                .put("client.transport.ignore_cluster_name", true)
                .build();

        try {
            TransportClient client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(new TransportAddress(InetAddress.getByName(host), port));

            Set<String> eventSet = new HashSet<>(Arrays.asList(events));
            return new EsClient(client, eventSet);
        } catch (Exception e) {
            log.error("Create es client exception: msg - {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String put(BlockHusk block) {
        if (!eventSet.contains(blockIndex)) {
            return null;
        }

        JsonObject jsonObject = block.toJsonObject();
        jsonObject.remove("body");

        String id = String.valueOf(block.getCoreBlock().getHeader().getIndex());
        IndexResponse response = client.prepareIndex(blockIndex, "_doc", id)
                .setSource(jsonObject.toString(), XContentType.JSON).get();

        switch (response.status()) {
            case OK:
            case CREATED:
                return id;
        }
        return null;
    }

    @Override
    public List<String> put(long blockNo, List<Transaction> transactions) {
        if (!eventSet.contains(txIndex) || transactions == null || transactions.size() == 0) {
            return null;
        }

        List<String> results = new ArrayList<>();
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        transactions.forEach(tx -> {
            String txHash = new TransactionHusk(tx).getHash().toString();
            results.add(txHash);

            JsonObject json = tx.toJsonObject();
            json.addProperty("blockNo", blockNo);

            bulkRequest.add(client.prepareIndex(txIndex, "_doc", txHash).setSource(json.toString(), XContentType.JSON));
        });

        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            return null;
        }
        return results;
    }
}
