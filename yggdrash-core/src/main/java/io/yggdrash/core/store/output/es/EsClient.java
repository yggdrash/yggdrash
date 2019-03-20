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
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

public class EsClient implements OutputStore {
    private static final Logger log = LoggerFactory.getLogger(EsClient.class);

    private static final String INDEX = "yggdrash";

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

            return new EsClient(client, events);
        } catch (Exception e) {
            log.error("Create es client exception: msg - {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String put(String schemeName, String id, JsonObject jsonObject) {
        IndexResponse response = client.prepareIndex(schemeName, "_doc", id)
                .setSource(jsonObject.toString(), XContentType.JSON).get();

        switch (response.status()) {
            case OK:
            case CREATED:
                return id;
        }
        return null;
    }

    @Override
    public void put(JsonObject block) {
        if (!eventSet.contains("block") || block == null) {
            return;
        }
        block.remove("body");

        String id = block.getAsJsonObject("header").get("index").getAsString();
        IndexResponse response = client.prepareIndex(INDEX, "block", id)
                .setSource(block.toString(), XContentType.JSON).get();

        switch (response.status()) {
            case OK:
            case CREATED:
                return;
            default:
                log.warn("Failed save block to elasticsearch");
        }
    }

    @Override
    public void put(String blockId, Map<String, JsonObject> transactionMap) {
        if (!eventSet.contains("tx") || transactionMap == null || transactionMap.size() == 0) {
            return;
        }

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        transactionMap.forEach((txHash, tx) -> {
            tx.addProperty("blockId", blockId);
            bulkRequest.add(client.prepareIndex(INDEX, "tx", txHash)
                    .setSource(tx.toString(), XContentType.JSON));
        });

        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            log.warn("Failed save transaction to elasticsearch");
        }
    }
}
