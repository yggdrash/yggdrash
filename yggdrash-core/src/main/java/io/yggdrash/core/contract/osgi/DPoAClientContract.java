package io.yggdrash.core.contract.osgi;

import io.yggdrash.core.blockchain.dpoa.Validator;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import java.util.List;

public class DPoAClientContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(io.yggdrash.core.contract.osgi.DPoAClientContract.class);

    // Bundle's context.
    private BundleContext bundleContext = null;
    // The service tacker object.
    private ServiceTracker serviceTracker = null;

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start dpoa client contract");

        bundleContext = context;
        serviceTracker = new ServiceTracker(
                bundleContext
                , bundleContext.createFilter(
                "(&(objectClass=" + DPoAContract.DPoAService.class.getName() + ")" +
                        "(YGGDRASH=DPoA))"),
                null
        );
        serviceTracker.open();

        Hashtable<String, String> props = new Hashtable();
        props.put("YGGDRASH", "DPoAClient");
        context.registerService(DPoAClientService.class.getName(), new DPoAClientService(serviceTracker), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("⚫ Stop dpoa client contract");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {

    }

    public static class DPoAClientService {
        private ServiceTracker serviceTracker;

        public DPoAClientService(ServiceTracker serviceTracker) {
            this.serviceTracker = serviceTracker;
        }

        @InvokeTransction
        public List<Validator> commit() {
            Object service = serviceTracker.getService();
            if (service == null || !(service instanceof DPoAContract.DPoAService)) {
                return null;
            }

            DPoAContract.DPoAService dPoAService = (DPoAContract.DPoAService) service;
            return dPoAService.commit();
        }
    }
}
