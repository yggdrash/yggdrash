package io.yggdrash.gateway.store.es;

import com.google.gson.JsonObject;
import io.yggdrash.contract.core.store.OutputStore;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class EsClient implements OutputStore {
    private static final Logger log = LoggerFactory.getLogger(EsClient.class);

    private static final String INDEX_PREFIX = "yggdrash-";

    private RestHighLevelClient client;

    private EsClient(RestHighLevelClient client) {
        this.client = client;
    }

    public static EsClient newInstance(String host, int port) {
        HttpHost httpHost = new HttpHost(host, port, "http");
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(httpHost));
        return new EsClient(client);
    }

    @Override
    public String put(String index, String id, JsonObject jsonObject) {
        try {
            IndexResponse response = client.index(new IndexRequest(index)
                    .id(id)
                    .source(jsonObject.toString(), XContentType.JSON), RequestOptions.DEFAULT);
            switch (response.status()) {
                case OK:
                case CREATED:
                    return id;
                default:
                    return null;
            }

        } catch (IOException e) {
            log.warn("Failed save {} to elasticsearch err={}", index, e.getMessage());
        }
        return null;
    }

    @Override
    public void put(JsonObject block) {
        if (block == null) {
            return;
        }
        block.remove("body");

        String index = INDEX_PREFIX + "block";
        String id = block.get("index").getAsString();
        put(index, id, block);
    }

    @Override
    public void put(String blockId, long blockIndex, Map<String, JsonObject> transactionMap) {
        if (transactionMap == null || transactionMap.size() == 0) {
            return;
        }
        String index = INDEX_PREFIX + "tx";
        BulkRequest bulkRequest = new BulkRequest();
        transactionMap.forEach((txHash, tx) -> {
            tx.addProperty("blockId", blockId);
            tx.addProperty("blockIndex", blockIndex);
            bulkRequest.add(new IndexRequest(index)
                    .id(txHash)
                    .source(tx.toString(), XContentType.JSON));
        });

        try {
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                String failureMessage = bulkResponse.buildFailureMessage();
                log.warn("Bulk response has failure={}", failureMessage);
            }
        } catch (IOException e) {
            log.warn("Failed save {} to elasticsearch err={}", index, e.getMessage());
        }
    }
}