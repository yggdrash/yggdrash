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

package io.yggdrash.core.blockchain;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrepareBlockchain {
    private static final Logger log = LoggerFactory.getLogger(PrepareBlockchain.class);

    DefaultConfig config;

    public PrepareBlockchain(DefaultConfig config) {
        this.config = config;
    }


    public boolean checkBlockChainIsReady(BlockChain blockChain) {
        // Get BranchContract
        List<BranchContract> contractList = blockChain.getBranchContracts();
        if(blockChain.getLastIndex() == 0 && contractList.size() == 0) {
            // is Genesis Blockchain
            contractList = blockChain.getBranchContracts();
        }

        // TODO check branch contract file exist
        if (contractList == null) {
            contractList = blockChain.getBranch().getBranchContracts();
        }
        if (contractList.size() == 0) {
            log.error("Branch Contract is Null");
        }

        for (BranchContract bc : contractList) {
            ContractVersion contractVersion = bc.getContractVersion();
            File contractFile = new File(String.format("%s/%s.jar", config.getContractPath(),
                    contractVersion));

            if (!(contractFile.exists() && contractFile.canRead())) {
                if(!findContractFile(contractVersion)) {
                    log.error("Contract %s is not find", contractVersion.toString());
                    return false;
                }
            }

            // verify contract file
            if (!verifyContractFile(contractFile, contractVersion)) {
                // TODO findContractFile
                log.error("Contract %s is not verify", contractVersion.toString());
                return false;
            }
        }
        return true;
    }

    public boolean findContractFile(ContractVersion contractVersion) {
        // TODO contract file download
        return false;
    }

    public boolean verifyContractFile(File contractFile, ContractVersion contractVersion) {
        // Contract Path + contract Version + .jar
        // check contractVersion Hex
        byte[] contractBinary;
        try {
            FileInputStream inputStream = new FileInputStream(contractFile);
            contractBinary = new byte[Math.toIntExact(contractFile.length())];
            inputStream.read(contractBinary);

            ContractVersion checkVersion = ContractVersion.of(contractBinary);
            return contractVersion.equals(checkVersion);

        } catch (IOException e) {
            log.warn(e.getMessage());
        }

        return false;

    }

}
