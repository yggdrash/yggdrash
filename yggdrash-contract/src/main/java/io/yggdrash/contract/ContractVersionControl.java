package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.contract.TxContractUpdatePropose;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.dpoa.DPoAContract;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

public class ContractVersionControl implements BundleActivator, ServiceListener {
    private static final Logger log = LoggerFactory.getLogger(ContractVersionControl.class);
    private BundleContext bundleContext = null;
    private ServiceTracker serviceTracker = null;

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("⚪ Start contract version control");

        bundleContext = context;
        serviceTracker = new ServiceTracker(
                bundleContext
                , bundleContext.createFilter(
                "(&(objectClass=" + DPoAContract.DPoAService.class.getName() + ")" +
                        "(YGGDRASH=DPoA))"),
                null
        );
        serviceTracker.open();

        Hashtable<String, String> props = new Hashtable<>();
        props.put("YGGDRASH", "ContractVersionControl");
        context.registerService(ContractVersionControlService.class.getName(), new ContractVersionControlService(serviceTracker), props);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("⚫ Stop contract version control");
    }

    @Override
    public void serviceChanged(ServiceEvent event) {

    }

    public static class ContractVersionControlService {
        private static final String SUFFIX_UPDATE_CONTRACT = "/update-temp-contracts";
        private static final String SUFFIX = ".jar";
        private final ServiceTracker serviceTracker;

        public ContractVersionControlService(ServiceTracker serviceTracker) {
            this.serviceTracker = serviceTracker;
        }

        @ContractStateStore
        ReadWriterStore<String, JsonObject> state;

        @ContractTransactionReceipt
        TransactionReceipt txReceipt;

        @InvokeTransaction
        public TransactionReceipt updateProposer(JsonObject params) throws UnsupportedEncodingException {
            if (validatorVerify()) {
                TxContractUpdatePropose txContractUpdatePropose = JsonUtil.generateJsonToClass(params.toString(), TxContractUpdatePropose.class);
                byte[] binaryFile = base64Dec(txContractUpdatePropose.getContract().getBytes("UTF-8"));

                if ((binaryFile == null) || sizeVerify(binaryFile)) {
                    return txReceipt;
                }
                FileOutputStream fos;

//            String containerPath = String.format("%s/%s", Path, branchId);
                String containerPath = "/Users/haewonwoo/woohae/yggdrash/yggdrash-core/.yggdrash/osgi/8b176b18903237a24d3cd4a5dc88feaa5a0dc746";
                String tempContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_UPDATE_CONTRACT);

                File fileDir = new File(tempContractPath);
                if (!fileDir.exists()) {
                    fileDir.mkdirs();
                }
                ContractVersion version = ContractVersion.of(binaryFile);
                File destFile = new File(tempContractPath + File.separator + version + SUFFIX);
                destFile.setReadOnly();

                try {
                    fos = new FileOutputStream(destFile);
                    fos.write(binaryFile);
                    fos.close();
                } catch (IOException e) {
                    log.error(e.toString());
                }

                txReceipt.setStatus(ExecuteStatus.SUCCESS);
            }
            return txReceipt;
        }

        public void updateStatus() {

        }

        private boolean validatorVerify() {
            DPoAContract.DPoAService dPoAService = (DPoAContract.DPoAService) serviceTracker.getService();
//            serviceTracker.waitForService(5000);
            dPoAService.getValidatorSet();

            ValidatorSet validatorSet = dPoAService.getValidatorSet();
            if (validatorSet == null || validatorSet.getValidatorMap() == null
                    || validatorSet.getValidatorMap().get(txReceipt.getIssuer()) == null) {
                return false;
            }
            return true;
        }

        private boolean sizeVerify(byte[] binaryFile) {
            if (binaryFile.length > 10) {
                return false;
            }
            return true;
        }

        private static byte[] base64Dec(byte[] buffer) {
            return Base64.decodeBase64(buffer);
        }
    }
}
