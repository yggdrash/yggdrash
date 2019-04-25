/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.gateway.dto.BranchDto;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class BranchDtoTest {

    private static final Logger log = LoggerFactory.getLogger(BranchDtoTest.class);

    @Test
    public void convertBranchDto() throws IOException {
        File genesisFile = new File(getClass().getClassLoader().getResource("./branch-yggdrash.json").getFile());

        String genesisString = FileUtil.readFileToString(genesisFile, FileUtil.DEFAULT_CHARSET);
        JsonObject branch = JsonUtil.parseJsonObject(genesisString);

        BranchDto dto = BranchDto.of(branch);
        log.debug("{}", dto);
        Assert.assertEquals(dto.name, branch.get("name").getAsString());
    }
}
