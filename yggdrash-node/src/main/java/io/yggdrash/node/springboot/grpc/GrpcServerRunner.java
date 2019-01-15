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

package io.yggdrash.node.springboot.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.stream.Stream;

public class GrpcServerRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

    private final GrpcServerBuilderConfigurer configurer;
    private final ServerBuilder<?> serverBuilder;

    @Autowired
    private AbstractApplicationContext applicationContext;

    private Server server;

    public GrpcServerRunner(GrpcServerBuilderConfigurer configurer, ServerBuilder<?> serverBuilder) {
        this.configurer = configurer;
        this.serverBuilder = serverBuilder;
    }

    @Override
    public void run(String... args) throws IOException {
        log.info("Starting gRPC Server ...");

        // find and register all GrpcService enabled beans
        getBeanNamesByTypeWithAnnotation(GrpcService.class, BindableService.class)
                .forEach(name -> {
                    BindableService service = applicationContext
                            .getBeanFactory().getBean(name, BindableService.class);
                    ServerServiceDefinition serviceDefinition = service.bindService();

                    //TODO bind global interceptors

                    //TODO add service to healthStatusManager

                    serverBuilder.addService(serviceDefinition);
                    log.info("'{}' service has been registered.", service.getClass().getName());
                });

        configurer.configure(serverBuilder);
        server = serverBuilder.build().start();

        log.info("gRPC Server started, listening on port {}.", server.getPort());
        startDaemonAwaitThread();
    }

    private <T> Stream<String> getBeanNamesByTypeWithAnnotation(
            Class<? extends Annotation> annotationType, Class<T> beanType) {
        return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name -> {
                    final BeanDefinition beanDefinition =
                            applicationContext.getBeanFactory().getBeanDefinition(name);
                    final Map<String, Object> beansWithAnnotation =
                            applicationContext.getBeansWithAnnotation(annotationType);

                    if (beansWithAnnotation.containsKey(name)) {
                        return true;
                    } else if (beanDefinition.getSource() instanceof AnnotatedTypeMetadata) {
                        // TODO need analysis
                        return ((AnnotatedTypeMetadata) beanDefinition.getSource())
                                .isAnnotated(annotationType.getName());
                    }

                    return false;
                });
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {
                GrpcServerRunner.this.server.awaitTermination();
            } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
}
