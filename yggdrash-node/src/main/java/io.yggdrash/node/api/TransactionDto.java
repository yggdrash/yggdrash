package io.yggdrash.node.api;

import com.google.common.primitives.Longs;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

public class TransactionDto {

    public Transaction jsonStringToTx(String jsonStr) throws ParseException {

        JSONParser parser = new JSONParser();
        JSONObject tx = (JSONObject) parser.parse(jsonStr);
        JSONObject header = (JSONObject) tx.get("header");
        JSONObject data = (JSONObject) tx.get("data");

        byte[] type = Hex.decode(header.get("type").toString().getBytes());
        byte[] version = Hex.decode((header.get("version").toString().getBytes()));
        byte[] dataHash = Hex.decode(header.get("dataHash").toString());
        long timestamp = Long.parseLong(header.get("timestamp").toString());
        long dataSize = Long.parseLong(header.get("dataSize").toString());
        byte[] signature = Hex.decode(header.get("signature").toString());
        String dataStr = data.toString();

        // ** Validation **

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(type, version, dataHash, timestamp, dataSize, signature);

        return new Transaction(txHeader, dataStr);
    }

    public Transaction jsonByteArrToTx(String jsonByteArr) throws ParseException {

        JSONParser parser = new JSONParser();
        JSONObject tx = (JSONObject) parser.parse(jsonByteArr);
        JSONObject header = (JSONObject) tx.get("header");

        byte[] type = Base64.decode(header.get("type").toString());
        byte[] version = Base64.decode(header.get("version").toString());
        byte[] dataHash = Base64.decode(header.get("dataHash").toString());
        long timestamp = (long) header.get("timestamp");
        long dataSize = (long) header.get("dataSize");
        byte[] signature = Base64.decode(header.get("signature").toString());
        JSONObject data = (JSONObject) tx.get("data");
        String dataStr = data.toString();

        // ** Validation **

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(type, version, dataHash, timestamp, dataSize, signature);

        return new Transaction(txHeader, dataStr);
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

        type = Arrays.copyOfRange(bytes, 0, typeLength);
        version = Arrays.copyOfRange(bytes, typeLength, typeLength + versionLength);
        dataHash = Arrays.copyOfRange(bytes, typeLength + versionLength,
                                               typeLength + versionLength + dataHashLength);
        timestamp = Arrays.copyOfRange(bytes, typeLength + versionLength + dataHashLength,
                                                typeLength + versionLength + dataHashLength + timestampLength);
        dataSize = Arrays.copyOfRange(bytes, typeLength + versionLength + dataHashLength + timestampLength,
                                               typeLength + versionLength + dataHashLength + timestampLength + dataSizeLength);
        signature = Arrays.copyOfRange(bytes, typeLength + versionLength + dataHashLength + timestampLength + dataSizeLength,
                                                typeLength + versionLength + dataHashLength + timestampLength + dataSizeLength + signatureLength);
        byte[] data = Arrays.copyOfRange(bytes, typeLength + versionLength + dataHashLength + timestampLength + dataSizeLength, txHeaderLength);

        Long timestampStr = Longs.fromByteArray(timestamp);
        Long dataSizeStr = Longs.fromByteArray(dataSize);
        String dataStr = new String(data);

        // ** Validation **

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(type, version, dataHash, timestampStr, dataSizeStr, signature);

        return new Transaction(txHeader, dataStr);
    }
}
