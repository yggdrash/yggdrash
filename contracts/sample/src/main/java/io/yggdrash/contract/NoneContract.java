package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

public class NoneContract implements BundleActivator {
    private static final Logger log = LoggerFactory.getLogger(NoneContract.class);

    @Override
    public void start(BundleContext context) {
        log.info("⚪ Start none contract");
        //Find for service in another bundle
        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "None");
        context.registerService(NoneService.class.getName(), new NoneService(), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop none contract");
    }

    public static class NoneService {
        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;


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