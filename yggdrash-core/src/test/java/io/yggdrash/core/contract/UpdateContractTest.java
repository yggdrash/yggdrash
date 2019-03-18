package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.contract.ContractVersionControl;
import io.yggdrash.core.blockchain.BranchId;
import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class UpdateContractTest {

    private static DefaultConfig defaultConfig = new DefaultConfig();

    @Test
    @Ignore
    public void updateTest() throws UnsupportedEncodingException {
        JsonObject params = createUpdateParams();
        ContractVersionControl.ContractVersionControlService service = new ContractVersionControl.ContractVersionControlService();
        service.updateProposer(params);
    }

    private JsonObject createUpdateParams() {
        BranchId branchId = BranchId.of("8b176b18903237a24d3cd4a5dc88feaa5a0dc746");
        String bash64String = ContractLoader.convertVersionToBase64String(defaultConfig.getOsgiPath(), branchId);

        JsonObject params = new JsonObject();
        params.addProperty("contract", bash64String);
        return params;
    }
}
