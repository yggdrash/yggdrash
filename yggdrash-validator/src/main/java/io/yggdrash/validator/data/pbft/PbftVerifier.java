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

package io.yggdrash.validator.data.pbft;

import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.consensus.ConsensusMessage;
import io.yggdrash.core.consensus.ConsensusMessageSet;
import io.yggdrash.core.consensus.ConsensusVerifier;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;

import java.util.Arrays;
import java.util.Map;

public class PbftVerifier implements ConsensusVerifier<PbftProto.PbftBlock, PbftMessage> {

    public static final PbftVerifier INSTANCE = new PbftVerifier();

    @Override
    public Boolean verify(ConsensusMessage<PbftMessage> pbftMessage) {
        if (pbftMessage == null
                || pbftMessage.getSignature() == null
                || pbftMessage.getSignature().length == 0) {
            return false;
        }

        // todo: check validator

        if (!Wallet.verify(pbftMessage.getHashForSigning(), pbftMessage.getSignature(), true)) {
            return false;
        }

        if (pbftMessage.getType().equals("PREPREPA")) {
            if (pbftMessage.getBlock() == null) {
                return false;
            }

            return Arrays.equals(pbftMessage.getHash(), pbftMessage.getBlock().getHash().getBytes())
                    && VerifierUtils.verify(pbftMessage.getBlock());
        }

        return true;
    }

    @Override
    public Boolean verify(ConsensusMessageSet<PbftMessage> pbftMessageSet) {
        PbftMessage prePrepare = pbftMessageSet.getPrePrepare();
        Map<String, PbftMessage> prepareMap = pbftMessageSet.getPrepareMap();
        Map<String, PbftMessage> commitMap = pbftMessageSet.getCommitMap();

        if (prePrepare == null || prePrepare.getSignature() == null
                || prepareMap == null
                || commitMap == null) {
            return false;
        }

        if (!verify(prePrepare)) {
            return false;
        }

        for (String key : prepareMap.keySet()) {
            PbftMessage pbftMessage = prepareMap.get(key);
            if (!verify(pbftMessage)) {
                return false;
            }
        }

        for (String key : commitMap.keySet()) {
            PbftMessage pbftMessage = commitMap.get(key);
            if (!verify(pbftMessage)) {
                return false;
            }
        }

        Map<String, PbftMessage> viewChangeMap = pbftMessageSet.getViewChangeMap();
        for (String key : viewChangeMap.keySet()) {
            PbftMessage pbftMessage = viewChangeMap.get(key);
            if (!verify(pbftMessage)) {
                return false;
            }
        }

        //todo : check 2f + 1 message count

        return true;
    }

    @Override
    public Boolean verify(ConsensusBlock<PbftProto.PbftBlock> pbftBlock) {
        if (pbftBlock == null || pbftBlock.getBlock() == null) {
            return false;
        } else if (pbftBlock.getIndex() == 0) {
            return VerifierUtils.verify(pbftBlock.getBlock());
        } else if (pbftBlock.getConsensusMessages() == null) {
            return false;
        } else {
            return VerifierUtils.verify(pbftBlock.getBlock())
                    && verify((PbftMessage) pbftBlock.getConsensusMessages());
        }
    }
}
