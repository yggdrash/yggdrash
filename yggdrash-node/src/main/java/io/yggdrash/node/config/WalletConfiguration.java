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

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.store.StoreBuilder;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class WalletConfiguration {

    @Bean
    DefaultConfig defaultConfig(Environment env) {
        boolean isProductionMode = Arrays.asList(env.getActiveProfiles()).contains("prod");
        return new DefaultConfig(isProductionMode);
    }

    @Bean
    StoreBuilder storeBuilder(DefaultConfig defaultConfig) {
        return new StoreBuilder(defaultConfig);
    }

    @Bean
    Wallet wallet(DefaultConfig defaultConfig) throws IOException, InvalidCipherTextException {
        return new Wallet(defaultConfig);
    }
}
