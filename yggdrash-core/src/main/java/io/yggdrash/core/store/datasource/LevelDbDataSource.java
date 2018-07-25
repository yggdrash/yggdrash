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

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

public class LevelDbDataSource implements DbSource {

    private static final Logger log = LoggerFactory.getLogger(LevelDbDataSource.class);
    private static final String DB_PATH = "resources/db/";

    private final ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

    private final String name;
    private DB db;

    public LevelDbDataSource(String name) {
        this.name = name;
    }

    public void init() {
        resetDbLock.writeLock().lock();
        try {
            log.debug("Initialize {} db", name);
            // TODO resource path set by profile or setting file
            Options options = new Options();
            options.createIfMissing(true);
            this.db = factory.open(new File(DB_PATH + name), options);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    public void put(byte[] key, byte[] value) {
        resetDbLock.writeLock().lock();

        try {
            db.put(key, value);
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    public byte[] get(byte[] key) {
        resetDbLock.writeLock().lock();
        try {
            return db.get(key);
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }
}
