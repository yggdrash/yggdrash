package io.yggdrash.core.store.output.es;

import com.google.gson.JsonObject;
import io.yggdrash.contract.core.store.OutputStore;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
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

    public static EsClient newInstance(String host, int port, Set<String> events) {
        Settings settings = Settings.builder()
                .put("client.transport.ignore_cluster_name", true)
                .build();

        try {
            TransportClient client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(new TransportAddress(InetAddress.getByName(host), port));

            Set<String> eventSet = events;
            return new EsClient(client, eventSet);
        } catch (Exception e) {
            log.error("Create es client exception: msg - {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String put(String schemeName, String id, JsonObject jsonObject) {
        IndexResponse response = client.prepareIndex(schemeName, "_doc", id)
                .setSource(jsonObject.toString(), XContentType.JSON).get();
        if (response.status() == RestStatus.OK || response.status() == RestStatus.CREATED) {
            return id;
        }
        return null;
    }

    @Override
    public void put(JsonObject block) {
        if (!eventSet.contains(blockIndex) || block == null) {
            return;
        }
        block.remove("body");

        String id = block.getAsJsonObject("header").get("index").getAsString();
        IndexResponse response = client.prepareIndex(blockIndex, "_doc", id)
                .setSource(block.toString(), XContentType.JSON).get();

        switch (response.status()) {
            case OK:
            case CREATED:
                return;
            default:
                log.warn("Failed save to elasticsearch");
        }
    }

    @Override
    public Set<String> put(long blockNo, Map<String, JsonObject> transactionMap) {
        if (!eventSet.contains(txIndex) || transactionMap == null || transactionMap.size() == 0) {
            return null;
        }

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        transactionMap.forEach((txHash, tx) -> {
            tx.addProperty("blockNo", blockNo);
            bulkRequest.add(client.prepareIndex(txIndex, "_doc", txHash).setSource(tx.toString(), XContentType.JSON));
        });

        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            return null;
        }
        return transactionMap.keySet();
    }
}
