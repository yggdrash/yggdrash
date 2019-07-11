/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.gateway.dto;

public class StatusDto {

    public String branchId;
    public long blockHeight;
    public long peerCount;

    public static StatusDto createBy(String branchId, long blockHeight, long peerCount) {
        StatusDto statusDto = new StatusDto();
        statusDto.branchId = branchId;
        statusDto.blockHeight = blockHeight;
        statusDto.peerCount = peerCount;
        return statusDto;
    }
}