package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.dpoa.DPoAContract;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.lang.reflect.Field;
import java.util.List;

public class UpdateContractTest {
    private ContractVersionControl.ContractVersionControlService service;
    private StateStore<JsonObject> store;
//    private static DefaultConfig defaultConfig = new DefaultConfig();
    private Field txReceiptField;

    public static class TestClass implements BundleActivator{
        private ServiceTracker serviceTracker;
        private ServiceRegistration serviceRegistration;
        private ContractVersionControl.ContractVersionControlService contractVersionControlService;

        @Override
        public void start(BundleContext context) throws Exception {
            serviceTracker = new ServiceTracker(context, DPoAContract.DPoAService.class.getName(), null);
            serviceTracker.open();
            ContractVersionControl.ContractVersionControlService service =
                    new ContractVersionControl.ContractVersionControlService(serviceTracker);
            serviceRegistration = context.registerService(DPoAContract.DPoAService.class.getName(), service, null);
            this.contractVersionControlService = service;
        }

        @Override
        public void stop(BundleContext context) throws Exception {
            // Unregister the ContractVersionControlService service
            serviceRegistration.unregister();
            // Close the ContractVersionControlService ServiceTracker
            serviceTracker.close();
        }

        public ContractVersionControl.ContractVersionControlService getContractVersionControlService() {
            return this.contractVersionControlService;
        }
    }

    @Before
    @Ignore
    public void setUp() throws IllegalAccessException {
        TestClass t = new TestClass();
        ContractVersionControl.ContractVersionControlService contractversionUpdateService = t.getContractVersionControlService();
        service = contractversionUpdateService;

        store = new StateStore<>(new HashMapDbSource());
        List<Field> txReceipt = ContractUtils.txReceiptFields(service);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }

        for (Field f : ContractUtils.contractFields(service, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(service, store);
        }
    }

    @Test
    @Ignore
    public void updateTest() throws Exception {

        String issuer = "a2b0f5fce600eb6c595b28d6253bed92be0568ed";
        TransactionReceipt preReceipt = new TransactionReceiptImpl();
        preReceipt.setIssuer(issuer);
        txReceiptField.set(service, preReceipt);

        JsonObject params = createUpdateParams();
        service.updateProposer(params);
    }

    private JsonObject createUpdateParams() {
//        BranchId branchId = BranchId.of("8b176b18903237a24d3cd4a5dc88feaa5a0dc746");
//        String bash64String = new String(ContractUtils.convertVersionToBase64(
//                "/Users/haewonwoo/woohae/yggdrash/yggdrash-core/.yggdrash/osgi", branchId));

        JsonObject params = new JsonObject();
        params.addProperty("contract", "8b176b18903237a24d3cd4a5dc88feaa5a0dc746");
        return params;
    }
}
