package io.yggdrash.common.store;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;

public class StateStore implements ReadWriterStore<String, JsonObject> {

    private final DbSource<byte[], byte[]> db;
    private long dbSize = 0L;
    private static final byte[] DATABASE_SIZE = "DATABASE_SIZE".getBytes();


    public StateStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        // getState Size
        if (db.get(DATABASE_SIZE) != null) {
            dbSize = Longs.fromByteArray(db.get(DATABASE_SIZE));
        }
    }

    public long getStateSize() {
        return dbSize;
    }
    
    @Override
    public void put(String key, JsonObject value) {
        // Check exist
        if (db.get(key.getBytes()) == null) {
            this.dbSize++;
            byte[] dbSizeByteArray = Longs.toByteArray(this.dbSize);
            db.put(DATABASE_SIZE, dbSizeByteArray);
        }
        byte[] tempValue = SerializationUtil.serializeJson(value);
        db.put(key.getBytes(), tempValue);
    }

    @Override
    public JsonObject get(String key) {
        byte[] result = db.get(key.getBytes());
        if (result == null) {
            return null;
        }
        String tempValue = SerializationUtil.deserializeString(result);
        return JsonUtil.parseJsonObject(tempValue);
    }

    @Override
    public boolean contains(String key) {
        return db.get(key.getBytes()) != null;
    }

    @Override
    public void close() {
        db.close();
    }


}
