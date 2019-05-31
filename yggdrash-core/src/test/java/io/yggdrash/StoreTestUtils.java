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

package io.yggdrash;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.core.store.StoreBuilder;

import java.nio.file.Paths;

public class StoreTestUtils {
    private static final String PATH = "testOutput";

    private StoreTestUtils() {}

    public static void clearDefaultConfigDb() {
        String dbPath = new DefaultConfig().getDatabasePath();
        FileUtil.recursiveDelete(Paths.get(dbPath));
    }

    public static void clearTestDb() {
        FileUtil.recursiveDelete(Paths.get(PATH));
    }

    public static String getTestPath() {
        return Paths.get(PATH, "store").toString();
    }

}
