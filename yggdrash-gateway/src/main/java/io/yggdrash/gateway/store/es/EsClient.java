package io.yggdrash.gateway.store.es;

import com.google.gson.JsonObject;
import com.google.protobuf.util.Timestamps;
import io.yggdrash.common.exception.FailedOperationException;
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
import java.net.UnknownHostException;
import java.util.Map;

public class EsClient implements OutputStore {
    private static final Logger log = LoggerFactory.getLogger(EsClient.class);

    private static final String INDEX = "yggdrash";

    private TransportClient client;

    private EsClient(TransportClient client) {
        this.client = client;
    }

    public static EsClient newInstance(String host, int port) {
        Settings settings = Settings.builder()
                .put("client.transport.ignore_cluster_name", true)
                .build();

        try (TransportClient client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(new TransportAddress(InetAddress.getByName(host), port))) {
            return new EsClient(client);
        } catch (UnknownHostException e) {
            throw new FailedOperationException(e);
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
            default:
                return null;
        }
    }

    @Override
    public void put(JsonObject block) {
        if (block == null) {
            return;
        }
        block.remove("body");

        String id = block.get("index").getAsString();
        IndexResponse response = client.prepareIndex(INDEX + "-block", "_doc", id)
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
        if (transactionMap == null || transactionMap.size() == 0) {
            return;
        }

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        transactionMap.forEach((txHash, tx) -> {
            tx.addProperty("blockId", blockId);
            long timestamp = tx.getAsJsonObject("header").get("timestamp").getAsLong();
            tx.addProperty("timestamp", Timestamps.fromMillis(timestamp).toString());

            bulkRequest.add(client.prepareIndex(INDEX + "-tx", "_doc", txHash)
                    .setSource(tx.toString(), XContentType.JSON));
        });

        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            String failureMessage = bulkResponse.buildFailureMessage();
            log.warn("Failed save transaction to elasticsearch err={}", failureMessage);
        }
    }
}