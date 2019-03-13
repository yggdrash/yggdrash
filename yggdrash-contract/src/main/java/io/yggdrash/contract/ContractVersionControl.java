package io.yggdrash.contract;

import io.yggdrash.contract.core.annotation.ContractQuery;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

public class ContractVersionControl implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(ContractVersionControl.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start contract version control");
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "ContractVersionControl");
        context.registerService(ContractVersionControlService.class.getName(), new ContractVersionControlService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop contract version control");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {

    }

    public static class ContractVersionControlService {
        @ContractQuery
        public void commit() {
        }
    }
}
