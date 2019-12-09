package io.yggdrash.node;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.gateway.dto.TransactionDto;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class TransactionDataFormater {

    private static final Logger log = LoggerFactory.getLogger(TransactionDataFormater.class);

    @Test
    public void testTransactionVerify() {
        String txJson =
                "{\"timestamp\":1561535579308,\"bodyLength\":156,\"body\":\"{\\\"method\\\":\\\"transfer\\\","
                        + "\\\"contractVersion\\\":\\\"6a2371e34b780dd39bd56002b1d96c23689cc5dc\\\","
                        + "\\\"params\\\":{\\\"to\\\":\\\"31e46b23c147f1276df3f3ed82d08a81fb679422\\\","
                        + "\\\"amount\\\":\\\"100\\\"}}\",\"branchId\":\"63589382e2e183e2a6969ebf57bd784dcb29bd43\","
                        + "\"type\":\"0000000000000000\",\"version\":\"0000000000000000\","
                        + "\"bodyHash\":\"41f06e437e0f5d7a1b28ba4e3f8a3ac252b6ee82e5c5886554dac53c514a48fd\","
                        + "\"signature\":\"1b79519d11c235058965486ed9a5b79629d83cee748527ddce31d496a63e6861783"
                        + "cfd0d5794e2af3bbec3440fdda6b8a5b794714322d9bc38a68f94b0f2756a03\"}";

        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(txJson).getAsJsonObject();
        TransactionDto dto = new TransactionDto();

        dto.branchId = obj.get("branchId").getAsString();
        dto.version = obj.get("version").getAsString();
        dto.type = obj.get("type").getAsString();
        dto.timestamp = obj.get("timestamp").getAsLong();
        dto.bodyHash = obj.get("bodyHash").getAsString();
        dto.bodyLength = obj.get("bodyLength").getAsLong();
        dto.signature = obj.get("signature").getAsString();
        dto.body = obj.get("body").getAsString();

        Transaction tx = TransactionDto.of(dto);
        String bodyHash = Hex.toHexString(HashUtil.sha3(tx.getTransactionBody().toBinary()));
        log.debug(bodyHash);

        assert VerifierUtils.verifyDataFormat(tx);

    }


}
