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

import io.yggdrash.util.FileUtil;
import org.junit.AfterClass;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static io.yggdrash.TestUtils.randomBytes;
import static org.assertj.core.api.Assertions.assertThat;

public class LevelDbDataSourceTest {
    private static final String dbPath = "testOutput";

    @AfterClass
    public static void destroy() {
        FileUtil.recursiveDelete(Paths.get(dbPath));
    }

    @Test
    public void shouldBeUpdateByBatch() {
        LevelDbDataSource ds = new LevelDbDataSource(dbPath, "batch-test");
        ds.init();

        Map<byte[], byte[]> rows = new HashMap<>();
        byte[] key = randomBytes(32);
        byte[] value = randomBytes(32);
        rows.put(key, value);
        rows.put(randomBytes(32), randomBytes(32));
        rows.put(randomBytes(32), randomBytes(32));
        rows.put(randomBytes(32), randomBytes(32));

        ds.updateByBatch(rows);
        byte[] foundValue = ds.get(key);
        assertThat(foundValue).isEqualTo(value);
    }

    @Test
    public void shouldBeReset() {
        LevelDbDataSource ds = new LevelDbDataSource(dbPath, "reset-test");
        ds.init();

        byte[] key = randomBytes(32);
        byte[] value = putDummyRow(ds, key);
        byte[] foundValue = ds.get(key);
        assertThat(foundValue).isEqualTo(value);

        ds.reset();

        foundValue = ds.get(key);
        assertThat(foundValue).isNull();
    }

    @Test
    public void shouldPutSomeThing() {
        LevelDbDataSource ds = new LevelDbDataSource(dbPath, "put-test");
        ds.init();

        byte[] key = randomBytes(32);
        byte[] value = putDummyRow(ds, key);
        byte[] foundValue = ds.get(key);
        assertThat(foundValue).isEqualTo(value);
    }

    private byte[] putDummyRow(LevelDbDataSource ds, byte[] key) {
        byte[] value = randomBytes(32);
        ds.put(key, value);

        return value;
    }

    @Test
    public void shouldInitialize() {
        String dbName = "initial-test";
        LevelDbDataSource ds = new LevelDbDataSource(dbPath, dbName);
        ds.init();

        assertThat(ds).isNotNull();
        assertThat(FileUtil.isExists(Paths.get(dbPath, dbName))).isTrue();
    }
}
