/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.springboot.grpc.context;

import io.yggdrash.node.springboot.grpc.autoconfigure.GrpcServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.SocketUtils;

import java.util.Properties;

public class GrpcServerEnvironment implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        Properties properties = new Properties();
        Integer configuredPort = environment.getProperty("yggdrash.node.grpc.port", Integer.class);

        if (configuredPort == null) {
            properties.put(LocalRunningGrpcPort.PROPERTY_NAME,
                    GrpcServerProperties.DEFAULT_GRPC_PORT);
        } else if (configuredPort == 0) {
            properties.put(LocalRunningGrpcPort.PROPERTY_NAME, SocketUtils.findAvailableTcpPort());
        } else {
            properties.put("yggdrash.node.grpc.port", configuredPort);
            properties.put(LocalRunningGrpcPort.PROPERTY_NAME, configuredPort);
        }

        propertySources.addLast(new PropertiesPropertySource("yggdrash", properties));
    }
}
