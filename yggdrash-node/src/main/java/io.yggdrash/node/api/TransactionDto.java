package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import org.spongycastle.util.Arrays;

import java.io.IOException;

public class TransactionDto {

    public Transaction jsonStringToTx(String jsonStr) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Transaction tx = mapper.readValue(jsonStr, Transaction.class);
        return tx;
    }

    public Transaction byteArrToTx(byte[] bytes) {

        byte[] type = new byte[4];
        byte[] version = new byte[4];
        byte[] dataHash = new byte[32];
        byte[] timestamp = new byte[8];
        byte[] dataSize = new byte[8];
        byte[] signature = new byte[65];

        int typeLength = type.length;
        int versionLength = version.length;
        int dataHashLength = dataHash.length;
        int timestampLength = timestamp.length;
        int dataSizeLength = dataSize.length;
        int signatureLength = signature.length;
        int txHeaderLength;
        txHeaderLength = typeLength + versionLength + dataHashLength + timestampLength
                        + dataHashLength + dataSizeLength + signatureLength;

        int sum = 0;
        type = Arrays.copyOfRange(bytes, sum, sum += typeLength);
        version = Arrays.copyOfRange(bytes, sum, sum += versionLength);
        dataHash = Arrays.copyOfRange(bytes, sum, sum += dataHashLength);
        timestamp = Arrays.copyOfRange(bytes, sum, sum += timestampLength);
        dataSize = Arrays.copyOfRange(bytes, sum, sum += dataSizeLength);
        signature = Arrays.copyOfRange(bytes, sum, sum += signatureLength);
        byte[] data = Arrays.copyOfRange(bytes, sum, txHeaderLength);

        Long timestampStr = Longs.fromByteArray(timestamp);
        Long dataSizeStr = Longs.fromByteArray(dataSize);
        String dataStr = new String(data);

        // ** Validation **

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(
                type, version, dataHash, timestampStr, dataSizeStr, signature);

        return new Transaction(txHeader, dataStr);
    }
}
