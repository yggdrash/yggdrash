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

package io.yggdrash.contract.dpoa;

import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.contract.core.annotation.ContractQuery;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class DPoAClientContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(DPoAClientContract.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start dpoa client contract");

        // Bundle's context.
        // The service tacker object.
        ServiceTracker serviceTracker = new ServiceTracker(
                context,
                context.createFilter(
                        "(&(objectClass=" + DPoAContract.DPoAService.class.getName() + ")"
                                + "(YGGDRASH=DPoA))"),
                null
        );
        serviceTracker.open();

        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "DPoAClient");
        context.registerService(DPoAClientService.class.getName(),
                new DPoAClientService(serviceTracker), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop dpoa client contract");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        log.info("serviceChanged called");
    }

    public static class DPoAClientService {
        private ServiceTracker serviceTracker;

        public DPoAClientService(ServiceTracker serviceTracker) {
            this.serviceTracker = serviceTracker;
        }

        @ContractQuery
        public List<Validator> commit() {
            Object service = serviceTracker.getService();
            if (!(service instanceof DPoAContract.DPoAService)) {
                return Collections.emptyList();
            }

            DPoAContract.DPoAService dpoaService = (DPoAContract.DPoAService) service;
            return dpoaService.commit(null);
        }
    }
}
