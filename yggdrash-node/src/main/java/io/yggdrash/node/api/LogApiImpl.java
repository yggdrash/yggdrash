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

package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AutoJsonRpcServiceImpl
public class LogApiImpl implements LogApi {

    private static final Logger log = LoggerFactory.getLogger(LogApiImpl.class);
    private static final String BRANCH_NOT_FOUND = "Branch not found";

    private final BranchGroup branchGroup;

    @Autowired
    public LogApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public Log getLog(String branchId, long index) {
        try {
            return branchGroup.getBranch(BranchId.of(branchId)).getContractManager().getLog(index);
        } catch (NullPointerException ne) {
            log.debug("GetLog Exception: {}", BRANCH_NOT_FOUND);
            return Log.createBy(BRANCH_NOT_FOUND);
        } catch (Exception e) {
            log.debug("GetLog Exception: {}", e.getMessage());
            return Log.createBy(e.getMessage());
        }
    }

    @Override
    public List<Log> getLogs(String branchId, long start, long offset) {
        try {
            return branchGroup.getBranch(BranchId.of(branchId)).getContractManager().getLogs(start, offset);
        } catch (NullPointerException ne) {
            log.debug("GetLogs Exception: {}", BRANCH_NOT_FOUND);
            return Collections.singletonList(Log.createBy(BRANCH_NOT_FOUND));
        } catch (Exception e) {
            log.debug("GetLogs Exception: {}", e.getMessage());
            return Collections.singletonList(Log.createBy(e.getMessage()));
        }
    }

    @Override
    public List<Log> getLogs(String branchId, String regex, long start, long offset) {
        return getLogs(branchId, start, offset).stream()
                .filter(l -> Pattern.compile(regex).matcher(l.getMsg()).find())
                .collect(Collectors.toList());
    }

    @Override
    public long curIndex(String branchId) {
        try {
            return branchGroup.getBranch(BranchId.of(branchId)).getContractManager().getCurLogIndex();
        } catch (NullPointerException ne){
            log.debug("CurIndex Exception: {}", BRANCH_NOT_FOUND);
            return 0;
        } catch (Exception e) {
            log.debug("CurIndex Exception : {}", e.getMessage());
            return 0;
        }
    }

}