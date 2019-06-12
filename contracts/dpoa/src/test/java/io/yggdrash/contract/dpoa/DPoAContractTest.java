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

package io.yggdrash.contract.dpoa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.ProposeValidatorSet;
import io.yggdrash.common.contract.vo.dpoa.ProposeValidatorSet.Votable.Vote;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.contract.vo.dpoa.tx.TxValidatorPropose;
import io.yggdrash.common.contract.vo.dpoa.tx.TxValidatorVote;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DPoAContractTest {
    private static final Logger log = LoggerFactory.getLogger(DPoAContractTest.class);
    private static final String ISSUER = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
    private static final String PROPOSED_VALIDATOR = "db0c9f45be6b121aaeef9e382320e0b156487b57";
    private static final String VALIDATOR_1 = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
    private static final String VALIDATOR_2 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";

    private DPoAContract.DPoAService dPoAService;
    private Field txReceiptField;
    private JsonArray validatorsArr;

    @Before
    public void setUp() throws IllegalAccessException {
        StateStore store = new StateStore(new HashMapDbSource());

        BranchStateStore branchStateStore = new BranchStateStore() {
            ValidatorSet validators;

            @Override
            public Long getLastExecuteBlockIndex() {
                return null;
            }

            @Override
            public Sha3Hash getLastExecuteBlockHash() {
                return null;
            }

            @Override
            public Sha3Hash getGenesisBlockHash() {
                return null;
            }

            @Override
            public Sha3Hash getBranchIdHash() {
                return null;
            }

            @Override
            public ValidatorSet getValidators() {
                return validators;
            }

            @Override
            public void setValidators(ValidatorSet validatorSet) {
                this.validators = validatorSet;
            }

            @Override
            public boolean isValidator(String address) {
                return validators.getValidatorMap().containsKey(address);
            }

            @Override
            public List<BranchContract> getBranchContacts() {
                return null;
            }

            @Override
            public String getContractVersion(String contractName) {
                return null;
            }

            @Override
            public String getContractName(String contractVersion) {
                return null;
            }

        };

        dPoAService = new DPoAContract.DPoAService();

        List<Field> txReceipt = ContractUtils.txReceiptFields(dPoAService);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }

        for (Field f : ContractUtils.contractFields(dPoAService, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(dPoAService, store);
        }

        for (Field f : ContractUtils.contractFields(dPoAService, ContractBranchStateStore.class)) {
            f.setAccessible(true);
            f.set(dPoAService, branchStateStore);
        }

        String validators = "{\"validator\": [\"a2b0f5fce600eb6c595b28d6253bed92be0568ed\""
                + ",\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\",\"d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95\"]}";
        JsonObject genesis = JsonUtil.parseJsonObject(validators);
        validatorsArr = genesis.getAsJsonArray("validator");

        boolean isSuccess = dPoAService.saveInitValidator(validatorsArr);
        assertTrue(isSuccess);
    }

    @Test
    public void saveInitValidator() {
        log.debug("saveInitValidator");
        String validator = "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95";
        JsonArray validators = new JsonArray();
        validators.add(validator);

        // Did not save init
        boolean isSuccess = dPoAService.saveInitValidator(validators);

        assertFalse(isSuccess);
        JsonObject query = new JsonObject();

        query.addProperty("address", validator);
        assertTrue(dPoAService.isValidator(query));
        query.addProperty("address", "1234");
        assertFalse(dPoAService.isValidator(query));
    }

    @Test
    public void proposeValidator() throws IllegalAccessException {
        log.debug("validator 추가 프로세스 start");
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);
        log.debug("proposeValidator {} , {} ", ISSUER, PROPOSED_VALIDATOR);

        TransactionReceipt receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        ProposeValidatorSet proposeValidatorSet = dPoAService.getProposeValidatorSet();
        assertNotNull(proposeValidatorSet);
        assertNotNull(proposeValidatorSet.getValidatorMap());
        assertNotNull(proposeValidatorSet.getValidatorMap().get(PROPOSED_VALIDATOR));

        // vote
        ProposeValidatorSet.Votable votable = proposeValidatorSet.getValidatorMap().get(PROPOSED_VALIDATOR);
        assertEquals(ISSUER, votable.getProposalValidatorAddr());
        assertEquals(0, votable.getAgreeCnt());
        assertEquals(0, votable.getDisagreeCnt());
        assertEquals(validatorsArr.size(), votable.getTotalVotableCnt());
        assertEquals(validatorsArr.size(), votable.getVotedMap().size());
        assertEquals(ProposeValidatorSet.Votable.VoteStatus.NOT_YET, votable.status());
    }

    /**
     * TxValidatorPropose Tx가 잘못된 경우.
     */
    @Test
    public void proposeValidatorFailTxValidation() throws IllegalAccessException {
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        TransactionReceipt receipt = dPoAService.proposeValidator(JsonUtil.parseJsonObject("{}"));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());
    }

    /**
     * Validator를 추천하는 Validator가 Validator Set에 존재하지 않는 경우.
     */
    @Test
    public void proposeValidatorFailTxValidationNotExists() throws IllegalAccessException {
        String issuer = "a809913b5a5193b477c51b4ba4aa0e1268ed6d13";
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(issuer);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);

        TransactionReceipt receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());
    }

    /**
     * 추천하는 Validator가 이미 추천 리스트에 존재하는 경우.
     */
    @Test
    public void proposeValidatorFailTxValidationAlreadyExists() throws IllegalAccessException {
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);

        TransactionReceipt receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        receipt = dPoAService.proposeValidator(JsonUtil.parseJsonObject(
                JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());
    }

    /**
     * Validator가 추천한 Validator에 대한 투표가 아직 완료되지 않아 새로운 Validator를 추천할 수 없는 경우.
     */
    @Test
    public void proposeValidatorFailTxValidationNotYetCompleteVoting() throws IllegalAccessException {
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);

        TransactionReceipt receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        tx = new TxValidatorPropose("51e5ae98cd821fa044d1eb49f03fb81a7acf3617");
        receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());
    }

    @Test
    public void voteValidator() throws IllegalAccessException {
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        //Propose validator
        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);
        TransactionReceipt receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        //Vote
        TxValidatorVote txValidatorVote = new TxValidatorVote(PROPOSED_VALIDATOR, true);
        TransactionReceipt votingReceipt = dPoAService.voteValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.SUCCESS, votingReceipt.getStatus());

        ProposeValidatorSet proposeValidatorSet = dPoAService.getProposeValidatorSet();
        assertNotNull(proposeValidatorSet);
        assertNotNull(proposeValidatorSet.getValidatorMap());
        assertNotNull(proposeValidatorSet.getValidatorMap().get(PROPOSED_VALIDATOR));

        ProposeValidatorSet.Votable votable = proposeValidatorSet.getValidatorMap().get(PROPOSED_VALIDATOR);
        assertEquals(ISSUER, votable.getProposalValidatorAddr());
        assertEquals(1, votable.getAgreeCnt());
        assertEquals(0, votable.getDisagreeCnt());
        assertEquals(validatorsArr.size(), votable.getTotalVotableCnt());
        assertEquals(validatorsArr.size(), votable.getVotedMap().size());
        assertEquals(ProposeValidatorSet.Votable.VoteStatus.NOT_YET, votable.status());
        assertTrue(votable.getVotedMap().get(ISSUER).isVoted());
        assertTrue(votable.getVotedMap().get(ISSUER).isAgree());
    }

    /**
     * TxValidatorVote Tx가 잘못된 경우.
     */
    @Test
    public void voteValidatorFailTxValidation() throws IllegalAccessException {
        //Vote
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        TransactionReceipt receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject("{}"));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());
    }

    /**
     * 투표하고자 하는 Validator가 제안된 Validator가 아닌 경우.
     */
    @Test
    public void voteValidatorNotExistsProposedValidator() throws IllegalAccessException {
        //Vote
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorVote txValidatorVote = new TxValidatorVote("51e5ae98cd821fa044d1eb49f03fb81a7acf3617", false);
        TransactionReceipt receipt = dPoAService.voteValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());

        //Propose validator
        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);
        receipt = dPoAService.proposeValidator(JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        //Vote
        receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());
    }

    /**
     * 투표에 참여할 수 없는 Validator가 투표를 진행하는 겨웅.
     */
    @Test
    public void voteValidatorNotAvailableVotingValidator() throws IllegalAccessException {
        //Vote
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        //Propose validator
        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);
        TransactionReceipt receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        //Vote
        String issuer = "33d2f8d22755e65fb0d92883f02413495ec3d9df";
        preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(issuer);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorVote txValidatorVote = new TxValidatorVote(PROPOSED_VALIDATOR, false);
        receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject(
                JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.FALSE, receipt.getStatus());
    }

    /**
     * 새로운 Validator 찬성 투표 결과에 의해 다음 블록검증에 참여할 Validator Set 업데이트.
     */
    @Test
    public void commitAddedValidator() throws IllegalAccessException {
        List<Validator> validators = dPoAService.commit();
        assertEquals(validatorsArr.size(), validators.size());
        assertEquals(VALIDATOR_1, validators.get(0).getAddr());
        assertEquals(VALIDATOR_2, validators.get(1).getAddr());
        assertEquals(ISSUER, validators.get(2).getAddr());

        //Propose validator
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(VALIDATOR_1);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);
        TransactionReceipt receipt = dPoAService.proposeValidator(JsonUtil.parseJsonObject(
                JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        //Vote (agree 1/3)
        preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(VALIDATOR_1);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorVote txValidatorVote = new TxValidatorVote(PROPOSED_VALIDATOR, true);
        receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        validators = dPoAService.commit();
        assertEquals(validatorsArr.size(), validators.size());
        assertEquals(VALIDATOR_1, validators.get(0).getAddr());
        assertEquals(VALIDATOR_2, validators.get(1).getAddr());
        assertEquals(ISSUER, validators.get(2).getAddr());

        //Vote (agree 2/3)
        preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(VALIDATOR_2);
        txReceiptField.set(dPoAService, preReceipt);

        txValidatorVote = new TxValidatorVote(PROPOSED_VALIDATOR, true);
        receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        validators = dPoAService.commit();
        assertEquals(4, validators.size());
        assertEquals(PROPOSED_VALIDATOR, validators.get(0).getAddr());
        assertEquals(VALIDATOR_1, validators.get(1).getAddr());
        assertEquals(VALIDATOR_2, validators.get(2).getAddr());
        assertEquals(ISSUER, validators.get(3).getAddr());

        ProposeValidatorSet.Votable votedHistory = validators.get(0).getVotedHistory();
        assertEquals(validatorsArr.size(), votedHistory.getTotalVotableCnt());
        assertEquals(2, votedHistory.getAgreeCnt());
        assertEquals(0, votedHistory.getDisagreeCnt());

        Map<String, Vote> votedMap = votedHistory.getVotedMap();
        for (Map.Entry<String, Vote> entry : votedMap.entrySet()) {
            switch (entry.getKey()) {
                case VALIDATOR_1:
                case VALIDATOR_2:
                    assertTrue(entry.getValue().isVoted());
                    assertTrue(entry.getValue().isAgree());
                    break;
                case ISSUER:
                    assertFalse(entry.getValue().isVoted());
                    assertFalse(entry.getValue().isAgree());
                    break;
                default:
                    break;
            }
        }


        ProposeValidatorSet proposeValidatorSet = dPoAService.getProposeValidatorSet();
        assertEquals(0, proposeValidatorSet.getValidatorMap().size());
    }

    /**
     * 새로운 Validator 반대 투표 결과에 의해 제안된 List 에서 제거.
     */
    @Test
    public void commitDisagreeValidator() throws IllegalAccessException {
        //Propose validator
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(VALIDATOR_1);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorPropose tx = new TxValidatorPropose(PROPOSED_VALIDATOR);
        TransactionReceipt receipt = dPoAService.proposeValidator(
                JsonUtil.parseJsonObject(JsonUtil.convertObjToString(tx)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        //Vote (disagree 1/3)
        preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(VALIDATOR_1);
        txReceiptField.set(dPoAService, preReceipt);

        TxValidatorVote txValidatorVote = new TxValidatorVote(PROPOSED_VALIDATOR, false);
        receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        List<Validator> validators = dPoAService.commit();
        assertEquals(3, validators.size());
        assertEquals(VALIDATOR_1, validators.get(0).getAddr());
        assertEquals(VALIDATOR_2, validators.get(1).getAddr());
        assertEquals(ISSUER, validators.get(2).getAddr());

        ProposeValidatorSet proposeValidatorSet = dPoAService.getProposeValidatorSet();
        assertEquals(1, proposeValidatorSet.getValidatorMap().size());

        //Vote (agree 2/3)
        preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(VALIDATOR_2);
        txReceiptField.set(dPoAService, preReceipt);

        txValidatorVote = new TxValidatorVote(PROPOSED_VALIDATOR, true);
        receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        validators = dPoAService.commit();
        assertEquals(3, validators.size());
        assertEquals(VALIDATOR_1, validators.get(0).getAddr());
        assertEquals(VALIDATOR_2, validators.get(1).getAddr());
        assertEquals(ISSUER, validators.get(2).getAddr());

        proposeValidatorSet = dPoAService.getProposeValidatorSet();
        assertEquals(1, proposeValidatorSet.getValidatorMap().size());

        //Vote (disagree 2/3, agree 1/3)
        preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(ISSUER);
        txReceiptField.set(dPoAService, preReceipt);

        txValidatorVote = new TxValidatorVote(PROPOSED_VALIDATOR, false);
        receipt = dPoAService.voteValidator(JsonUtil.parseJsonObject(JsonUtil.convertObjToString(txValidatorVote)));
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        validators = dPoAService.commit();
        assertEquals(3, validators.size());
        assertEquals(VALIDATOR_1, validators.get(0).getAddr());
        assertEquals(VALIDATOR_2, validators.get(1).getAddr());
        assertEquals(ISSUER, validators.get(2).getAddr());

        proposeValidatorSet = dPoAService.getProposeValidatorSet();
        assertEquals(0, proposeValidatorSet.getValidatorMap().size());
    }

}
