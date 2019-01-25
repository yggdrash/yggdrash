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
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GrpcServerRunner implements CommandLineRunner, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

    private final GrpcServerBuilderConfigurer configurer;
    private final ServerBuilder<?> serverBuilder;

    private AbstractApplicationContext applicationContext;

    private Server server;

    public GrpcServerRunner(GrpcServerBuilderConfigurer configurer, ServerBuilder<?> serverBuilder) {
        this.configurer = configurer;
        this.serverBuilder = serverBuilder;
    }

    @Autowired
    public void setApplicationContext(AbstractApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Server getServer() {
        return server;
    }

    @Override
    public void run(String... args) throws IOException {
        log.info("Starting gRPC Server ...");

        // find global Interceptors
        List<ServerInterceptor> globalInterceptors =
                getBeanNamesByTypeWithAnnotation(GrpcGlobalInterceptor.class, ServerInterceptor.class)
                        .map(name -> applicationContext.getBeanFactory().getBean(name, ServerInterceptor.class))
                        .collect(Collectors.toList());

        // find and register all GrpcService enabled beans
        getBeanNamesByTypeWithAnnotation(GrpcService.class, BindableService.class)
                .forEach(name -> {
                    BindableService service = applicationContext
                            .getBeanFactory().getBean(name, BindableService.class);
                    ServerServiceDefinition serviceDefinition = service.bindService();

                    //bind global and private interceptors
                    GrpcService grpcServiceAnnotation =
                            applicationContext.findAnnotationOnBean(name, GrpcService.class);
                    serviceDefinition = bindInterceptors(
                            serviceDefinition, grpcServiceAnnotation, globalInterceptors);

                    //TODO add service to healthStatusManager

                    serverBuilder.addService(serviceDefinition);
                    log.info("'{}' service has been registered.", service.getClass().getName());
                });

        configurer.configure(serverBuilder);
        server = serverBuilder.build().start();

        log.info("gRPC Server started, listening on port {}.", server.getPort());
        startDaemonAwaitThread();
    }

    private ServerServiceDefinition bindInterceptors(
            ServerServiceDefinition serviceDefinition, GrpcService grpcService,
            Collection<ServerInterceptor> globalInterceptors) {

        Stream<? extends ServerInterceptor> privateInterceptors =
                Stream.of(grpcService.interceptors()).map(interceptorClass -> {
                    try {
                        return 0 < applicationContext.getBeanNamesForType(interceptorClass).length
                                ? applicationContext.getBean(interceptorClass) :
                                interceptorClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new BeanCreationException("Failed to create interceptor instance.", e);
                    }
                });

        List<ServerInterceptor> interceptors = Stream.concat(
                grpcService.applyGlobalInterceptors() ? globalInterceptors.stream() :
                        Stream.empty(), privateInterceptors)
                .distinct()
                .collect(Collectors.toList());
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
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

    /**
     * Stop serving requests and shutdown resources.
     */
    @Override
    public void destroy() {
        if (server != null) {
            server.shutdown();
        }
    }
}
