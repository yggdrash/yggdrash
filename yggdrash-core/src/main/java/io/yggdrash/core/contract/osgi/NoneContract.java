package io.yggdrash.core.contract.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.InvokeTransaction;
import io.yggdrash.core.runtime.annotation.YggdrashContract;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.Store;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@YggdrashContract
public class NoneContract implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(io.yggdrash.core.contract.osgi.NoneContract.class);

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start none contract");
        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable();
        props.put("YGGDRASH", "None");
        context.registerService(NoneService.class.getName(), new NoneService(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("⚫ Stop none contract");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {

    }


    public static class NoneService implements Contract {
        @ContractStateStore
        Store<String, JsonObject> state;


        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        public void init(StateStore stateStore) {
        }

        @InvokeTransaction
        public boolean doNothing(JsonObject param) {
            // pass
            return true;
        }

        @ContractQuery
        public String someQuery() {
            return "";
        }
    }
}
