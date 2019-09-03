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

package io.yggdrash.mock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ContractMock {

    JsonObject mock = new JsonObject();

    String validatorAddress = "";

    String name = "MOCK";
    String symbol = "MOCK";
    String property = "MOCK";
    String description = "MOCK";
    String timeStamp = "000001674dc56231";
    JsonObject consensus = new JsonObject();
    String governanceContract = "DpoA";

    JsonArray contracts = new JsonArray();

    JsonObject sampleContract = new JsonObject();
    JsonObject governanceSampleContract = new JsonObject();
    String contractVersion = "a88ae404e837cd1d6e8b9a5a91f188da835ccb56";
    JsonObject contractInit = new JsonObject();
    String contractDescription = "MOCK";
    String contractName = "MOCK";
    boolean contractIsSystem = false;

    public ContractMock setValidatorAddress(String validatorAddress) {
        this.validatorAddress = validatorAddress;
        return this;
    }

    public ContractMock setName(String name) {
        this.name = name;
        return this;
    }

    public ContractMock setSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public ContractMock setProperty(String property) {
        this.property = property;
        return this;
    }

    public ContractMock setDescription(String description) {
        this.description = description;
        return this;
    }

    public ContractMock setGovernanceContract(String governance) {
        this.governanceContract = governance;
        return this;
    }

    public ContractMock setContractVersion(String contractVersion) {
        this.contractVersion = contractVersion;
        return this;
    }

    public ContractMock setContractInit(JsonObject init) {
        this.contractInit = init;
        return this;
    }

    public ContractMock setContractDescription(String description) {
        this.contractDescription = description;
        return this;
    }

    public ContractMock setContractName(String name) {
        this.contractName = name;
        return this;
    }

    public ContractMock setContractIsSystem(boolean isSystem) {
        this.contractIsSystem = isSystem;
        return this;
    }

    public ContractMock setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    public ContractMock build() {
        createSampleContract();
        createGovernanceSampleContract();
        createContracts();
        createConsensus();
        createMock();
        return this;
    }

    public void createSampleContract() {
        sampleContract.addProperty("contractVersion", contractVersion);
        sampleContract.add("init", contractInit);
        sampleContract.addProperty("description", contractDescription);
        sampleContract.addProperty("name", contractName);
        sampleContract.addProperty("isSystem", contractIsSystem);
    }

    public void createGovernanceSampleContract() {
        JsonObject initObj = new JsonObject();
        JsonArray validators = new JsonArray();
        validators.add(validatorAddress);
        initObj.add("validators", validators);
        governanceSampleContract.addProperty("contractVersion", "ca3c8385d8323f28280352f20a9e39f0e40837b9");
        governanceSampleContract.add("init", initObj);
        governanceSampleContract.addProperty("description", "This contract is for a validator.");
        governanceSampleContract.addProperty("name", governanceContract);
        governanceSampleContract.addProperty("isSystem", true);
    }

    public void createContracts() {
        contracts.add(sampleContract);
        contracts.add(governanceSampleContract);
    }

    public void createConsensus() {
        consensus.addProperty("algorithm", "pbft");
        consensus.addProperty("period", "* * * * * *");
    }

    public void createMock() {
        mock.addProperty("name", name);
        mock.addProperty("symbol", symbol);
        mock.addProperty("property", property);
        mock.addProperty("description", description);
        mock.add("contracts", contracts);
        mock.addProperty("timestamp", timeStamp);
        mock.add("consensus", consensus);
        mock.addProperty("governanceContract", governanceContract);
    }

    public JsonObject mock() {
        return mock;
    }

    public static ContractMock builder() {
        return new ContractMock();
    }

}
