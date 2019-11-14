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

package io.yggdrash.core.blockchain.osgi;

import com.google.common.base.Strings;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Downloader {

    private static Logger log = LoggerFactory.getLogger(Downloader.class);

    private static String contractRepoUrl = "http://store.yggdrash.io/contract/";
    private static String contractFilePath;

    public Downloader(DefaultConfig defaultConfig) {
        contractRepoUrl = defaultConfig.getContractRepositoryUrl();
        contractFilePath = defaultConfig.getContractPath();
    }

    private static String filePathBuilder(String path, ContractVersion contractVersion) {
        return path + File.separator + contractVersion + ".jar";
    }

    static File downloadContract(ContractVersion version) throws IOException {
        return downloadContract(contractFilePath, version);
    }

    public static File downloadContract(String path, ContractVersion version) {
        mkdir(path);

        String filePath = filePathBuilder(path, version);
        int bufferSize = 1024;

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath))) {
            log.info("-------Download Contract Start------");
            URL url = new URL(contractRepoUrl + version + ".jar");
            byte[] buf = new byte[bufferSize];
            int byteWritten = 0;

            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();

            int byteRead;
            while ((byteRead = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, byteRead);
                byteWritten += byteRead;
            }

            log.info("Download Contract Successfully. ContractVersion : {}\t of bytes : {}", version, byteWritten);
            log.info("-------Download Contract End--------");
        } catch (IOException e) {
            log.error("Download contract file failed, {}", e.getMessage());
            new File(filePath).delete();
        }
        return new File(filePath);
    }

    public static boolean verifyUrl(ContractVersion contractVersion) {
        try {
            URL url = new URL(contractRepoUrl + contractVersion + ".jar");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("HEAD");
            connection.connect();

            int responseCode = connection.getResponseCode();

            log.info("VerifyUrl : url={}, responseCode={}", url.toString(), responseCode);
            return responseCode == 200;

        } catch (MalformedURLException e) {
            log.info(e.getMessage());
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        return false;
    }

    private static void mkdir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

}
