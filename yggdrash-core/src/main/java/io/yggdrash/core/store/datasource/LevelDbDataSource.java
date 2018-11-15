/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store.datasource;

import io.yggdrash.common.util.FileUtil;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

public class LevelDbDataSource implements DbSource<byte[], byte[]> {

    private static final Logger log = LoggerFactory.getLogger(LevelDbDataSource.class);

    private final ReadWriteLock resetDbLock = new ReentrantReadWriteLock();
    private final String name;
    private final String dbPath;

    private boolean alive;
    private DB db;

    public LevelDbDataSource(String dbPath, String name) {
        this.dbPath = dbPath;
        this.name = name;
    }

    public DbSource<byte[], byte[]> init() {
        resetDbLock.writeLock().lock();
        try {
            log.info("Initialize db: {}", name);

            if (isAlive()) {
                log.warn("DbSource is alive.");
            }

            if (name == null) {
                throw new NullPointerException("no name set to the dbStore");
            }

            // TODO resource path set by profile or setting file
            Options options = new Options();
            options.createIfMissing(true);
            openDb(options);
            alive = true;
        } catch (IOException e) {
            throw new RuntimeException("Can't initialize db");
        } finally {
            resetDbLock.writeLock().unlock();
        }

        return this;
    }

    private void openDb(Options options) throws IOException {
        try {
            db = factory.open(getDbFile(), options);
        } catch (IOException e) {
            if (e.getMessage().contains("Corruption:")) {
                factory.repair(getDbFile(), options);
                db = factory.open(getDbFile(), options);
            } else {
                throw e;
            }
        }
    }

    private File getDbFile() {
        return getDbPath().toFile();
    }

    private Path getDbPath() {
        return Paths.get(dbPath, name);
    }

    public void put(byte[] key, byte[] value) {
        resetDbLock.readLock().lock();
        try {
            db.put(key, value);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    public byte[] get(byte[] key) {
        resetDbLock.readLock().lock();
        try {
            return db.get(key);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    void updateByBatch(Map<byte[], byte[]> rows) {
        resetDbLock.readLock().lock();
        try {
            WriteBatch batch = db.createWriteBatch();
            rows.forEach((key, value) -> {
                if (value == null) {
                    batch.delete(key);
                } else {
                    batch.put(key, value);
                }
            });
            db.write(batch);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    void reset() {
        close();
        FileUtil.recursiveDelete(getDbPath());
        init();
    }

    @Override
    public synchronized void close() {
        if (!isAlive()) {
            return;
        }

        try {
            log.debug("Close db: {}", name);
            db.close();
            alive = false;
        } catch (IOException e) {
            log.error("Failed to find the db file on the close: {}", name);
        }
    }

    @Override
    public void delete(byte[] key) {
        resetDbLock.readLock().lock();
        try {
            db.delete(key);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    private boolean isAlive() {
        return alive;
    }

    public List<byte[]> getAll() throws IOException {
        List<byte[]> valueList = new ArrayList<>();
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                byte[] value = iterator.peekNext().getValue();
                valueList.add(value);
            }
        }
        return valueList;
    }
}
