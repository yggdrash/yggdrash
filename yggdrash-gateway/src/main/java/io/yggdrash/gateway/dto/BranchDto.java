/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import io.yggdrash.core.exception.WrongStructuredException;

import java.util.List;
import java.util.Map;

/**
 * Deserialized Branch object from jsonFile
 */
public class BranchDto {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String name;
    public String symbol;
    public String property;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String type;
    public String description;
    // TODO Change Contract Ids (as List)
    public List<Map<String,Object>> contracts;
    public String timestamp;

    public Map<String, Object> consensus;

    public static BranchDto of(JsonObject json) {
        try {
            return MAPPER.readValue(json.toString(), BranchDto.class);
        } catch (Exception e) {
            throw new WrongStructuredException.InvalidBranch();
        }
    }

}
