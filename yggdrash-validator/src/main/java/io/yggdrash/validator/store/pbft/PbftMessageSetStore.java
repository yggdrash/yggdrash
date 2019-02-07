package io.yggdrash.validator.store.pbft;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.datasource.DbSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PbftMessageSetStore implements Store<byte[], JsonObject> {
    private static final Logger log = LoggerFactory.getLogger(PbftMessageSetStore.class);

    private final DbSource<byte[], byte[]> db;

    public PbftMessageSetStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(byte[] key, JsonObject value) {
        log.trace("put "
                + "(key: " + Hex.toHexString(key) + ")"
                + "(value length: " + value.toString().length() + ")");
        db.put(key, value.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public JsonObject get(byte[] key) {
        byte[] foundValue = db.get(key);
        log.trace("get "
                + "(" + Hex.toHexString(key) + ") "
                + "value size: "
                + foundValue.length);
        if (foundValue.length > 0) {
            return JsonUtil.parseJsonObject(new String(foundValue, StandardCharsets.UTF_8));
        }
        throw new NonExistObjectException("Not Found [" + Hex.toHexString(key) + "]");
    }

    @Override
    public boolean contains(byte[] key) {
        if (key != null) {
            return db.get(key) != null;
        }

        return false;
    }

    public int size() {
        try {
            return db.getAll().size();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return 0;
    }

    public void close() {
        this.db.close();
    }
}