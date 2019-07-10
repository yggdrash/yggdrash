/*
 * Copyright 2018 Akashic Foundation
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

package io.yggdrash.node.config;

import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.Constants.ActiveProfiles;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

import static java.lang.System.exit;

@Configuration
public class WalletConfiguration {
    private static final Logger log = LoggerFactory.getLogger(WalletConfiguration.class);

    @Bean
    DefaultConfig defaultConfig(Environment env) {
        boolean isProductionMode = Arrays.asList(env.getActiveProfiles()).contains(ActiveProfiles.PROD);
        DefaultConfig defaultConfig = new DefaultConfig(isProductionMode);
        log.info("Yggdrash Data Path : {}", defaultConfig.getYggDataPath());
        return defaultConfig;
    }

    @Bean
    Wallet wallet(DefaultConfig defaultConfig) {
        Wallet wallet = null;
        try {
            wallet = new Wallet(defaultConfig);
        } catch (Exception e) {
            log.debug(defaultConfig.getString(Constants.PROPERTY_KEYPATH));
            log.error("Key Password is not valid.");
            exit(0);
        }
        return wallet;
    }
}
