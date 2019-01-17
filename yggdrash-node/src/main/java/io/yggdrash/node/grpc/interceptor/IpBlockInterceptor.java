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

package io.yggdrash.node.grpc.interceptor;

import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;

public class IpBlockInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger("interceptor.ipBlock");
    private String[] blackIps = {};

    public void setBlackIps(String[] ips) {
        this.blackIps = ips;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Iterator<String> iterator = headers.keys().iterator();
        iterator.forEachRemaining(key -> {
            log.info("header:{}:{}", key, headers.get(Metadata.Key.of(key,
                    Metadata.ASCII_STRING_MARSHALLER)));
        });

        if (isBlocked(getRemoteHost(getRemoteInetSocketString(call)))) {
            call.close(Status.ABORTED, headers);
        }

        return next.startCall(call, headers);
    }

    private String getRemoteHost(String remoteInetSocketString) {
        return remoteInetSocketString.substring(1, remoteInetSocketString.lastIndexOf(':'));
    }

    private boolean isBlocked(String host) {
        return Arrays.asList(blackIps).contains(host);
    }

    private <ReqT, RespT> String getRemoteInetSocketString(ServerCall<ReqT, RespT> call) {
        return call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString();
    }
}
