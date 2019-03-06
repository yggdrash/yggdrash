/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.contract.methods.ContractMethod;
import org.apache.commons.io.FileUtils;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ContractManager extends ClassLoader {
    private static final String SUFFIX = ".class";

    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);
    private Map<ContractVersion, ContractMeta> contracts = new HashMap<>();
    private String contractPath;

    public ContractManager(String contractPath) {
        this.contractPath = contractPath;
        load();
    }

    private void load() {
        File targetDir = new File(this.contractPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("The contract file does not exist" + targetDir.getAbsolutePath());
        }
        log.info("ContractManager load path : {} ", contractPath);
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(this.contractPath)))) {
            filePathStream.forEach(p -> {
                File contractFile = new File(String.valueOf(p));
                if (contractFile.isDirectory()) {
                    return;
                }
                if (!contractFile.getName().endsWith(".class")) {
                    return;
                }

                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    ContractVersion contractVersion = ContractVersion.of(contractBinary);
                    try{
                        ContractMeta contractMeta = ContractClassLoader.loadContractByVersion(
                                this.contractPath, contractVersion);
                        if (Files.isRegularFile(p) && validation(contractMeta)) {
                            contracts.put(contractVersion, contractMeta);
                        }
                    }catch (Throwable e){

                    }
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<ContractVersion, ContractMeta> getContracts() {
        return this.contracts;
    }

    public List<ContractVersion> getContractVersionList() {
        return new ArrayList<>(this.contracts.keySet());
    }

    public List<ContractMeta> getContractList() {
        return new ArrayList<>(this.contracts.values());
    }

    public ContractMeta getContractByVersion(ContractVersion version) {
        return this.contracts.get(version);
    }

    public Boolean hasContract(ContractVersion version) {
        return this.contracts.containsKey(version);
    }

    public Set<JsonElement> getMethod(ContractVersion version) {
        if (!hasContract(version)) {
            return Collections.emptySet();
        }
        ContractMeta contractMeta = contracts.get(version);
        Set<JsonElement> methodSet = new HashSet<>();

        JsonArray qm = contractMeta.toJsonObject().getAsJsonArray("queryMethods");
        JsonArray im = contractMeta.toJsonObject().getAsJsonArray("invokeMethods");
        for (JsonElement q : qm) {
            methodSet.add(q);
        }
        for (JsonElement i : im) {
            methodSet.add(i);
        }
        return methodSet;
    }

    /**
     * Check the requirements required by the contract
     */
    public Boolean validation(ContractMeta contractMeta) {
        if (contractMeta.getStateStore() == null) {
            throw new IllegalArgumentException("Contract does not have required filed state store.");
        }
        if (contractMeta.getTxReceipt() == null) {
            throw new IllegalArgumentException("Contract does not have required filed transaction receipt.");
        }
        if (this.contracts.containsKey(contractMeta.getContractVersion())) {
            throw new IllegalArgumentException("The contract version already exists.");
        }

        for (Map.Entry<String, ContractMethod> elem :
                contractMeta.getQueryMethods().entrySet()) {
            if (elem.getValue().getMethod().getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException("The query method should not return void.");
            }
        }

        for (Map.Entry<String, ContractMethod> elem :
                contractMeta.getInvokeMethods().entrySet()) {
            if (elem.getValue().getMethod().getParameterTypes().length < 1) {
                throw new IllegalArgumentException("Invoke method must have parameters");
            }
        }

        //TODO whitelist sandBox validation
        // Check the Project Jigsaw
        return true;
    }

    /**
     * Add a contract that the manager does not have
     * or Adding a contract from contractRequest
     */
    public void addContract(Class<? extends Contract> contract) {
        //TODO check the node admin
        File targetDir = new File(contractPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("The contract file does not exist" + targetDir.getAbsolutePath());
        }
        ContractMeta contractMeta = ContractClassLoader.loadContractClass(contract);
        ContractVersion contractVersion = contractMeta.getContractVersion();
        if (validation(contractMeta)) {
            File contractFile = ContractMeta.contractFile(contractPath, contractVersion);
            if (!contractFile.exists()) {
                try {
                    FileUtils.writeByteArrayToFile(contractFile, contractMeta.getContractClassBinary());
                    this.contracts.put(contractVersion, contractMeta);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public Boolean removeContract(ContractVersion contractVersion) {
        //TODO check the node admin
        String directoryPath = contractVersion.toString().substring(0, 2);
        File file = new File(contractPath + File.separator + getFilePath(contractVersion));
        File directory = new File(contractPath + File.separator + directoryPath);
        if (existFile(contractVersion)) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                Arrays.stream(files).forEach(File::delete);
            }
            file.delete();
            directory.delete();
            this.contracts.remove(contractVersion);
            return true;
        }
        return false;
    }

    /**
     * Change the contract to the contract version.
     */
    public ContractVersion convertContractToVersion(Class<? extends Contract> contract) {
        ContractMeta contractMeta = ContractClassLoader.loadContractClass(contract);
        return contractMeta.getContractVersion();
    }

    /**
     * Decodinging a contract file
     */
    public String decodingContract(String encodedString) throws UnsupportedEncodingException {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decodedBytes = decoder.decode(encodedString);
        ContractVersion contractVersion = ContractVersion.of(new String(decodedBytes, "UTF-8"));
        return decompileContract(contractVersion);
    }

    /**
     * Decompile a contract class
     */
    public String decompileContract(ContractVersion contractVersion) {
        if (!existFile((contractVersion))) {
            //TODO reuqest to another node
            log.error("The contract file does not exist");
        }
        AtomicReference<String> result = new AtomicReference<>("");
        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType,
                                                     Collection<SinkClass> collection) {
                if (sinkType == SinkType.JAVA && collection.contains(SinkClass.DECOMPILED)) {
                    return Arrays.asList(SinkClass.DECOMPILED, SinkClass.STRING);
                } else {
                    return Collections.singletonList(SinkClass.STRING);
                }
            }

            Consumer<SinkReturns.Decompiled> dumpDecompiled = d -> {
                result.set(d.getJava());
            };

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                    return x -> dumpDecompiled.accept((SinkReturns.Decompiled) x);
                }
                return ignore -> {};
            }
        };

        CfrDriver driver = new CfrDriver.Builder().withOutputSink(mySink).build();
        driver.analyse(Collections.singletonList(
                contractPath + File.separator + getFilePath(contractVersion)));
        return result.get();
    }

    private String getFilePath(ContractVersion contractVersion) {
        return contractVersion.toString().substring(0, 2) + File.separator
                + contractVersion + SUFFIX;
    }

    private Boolean existFile(ContractVersion contractVersion) {
        File file = new File(contractPath + File.separator + getFilePath(contractVersion));
        if (!file.exists()) {
            return false;
        }
        return true;
    }

    public static byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                log.error(e.getCause().toString());
            }
        }
        return bos.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    //TODO
    // contract request to another node

}
