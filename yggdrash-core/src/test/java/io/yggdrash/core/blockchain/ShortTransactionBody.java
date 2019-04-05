package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.TestConstants;
import io.yggdrash.common.contract.ContractVersion;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortTransactionBody {

    private static final Logger log = LoggerFactory.getLogger(ShortTransactionBody.class);

    @Before
    public void setUp() {
        TestConstants.yggdrash();
    }


    @Test
    public void shortTransactionBody() {
        TransactionBuilder builder = new TransactionBuilder();
        ContractVersion version1 = TestConstants.YEED_CONTRACT;
        log.debug(version1.toString());

        ContractVersion version2 = ContractVersion.of(version1.toString().substring(0,8));
        JsonObject param = new JsonObject();
        param.addProperty("to", TestConstants.TRANSFER_TO);
        param.addProperty("amount", 100);

        Transaction tx1 = builder.addTxBody(version1, "testInvoke", param)
                .setBranchId(TestConstants.yggdrash())
                .setWallet(TestConstants.wallet())
                .buildTransaction();

        builder = new TransactionBuilder();

        Transaction tx2 = builder.addTxBody(version2, "testInvoke", param)
                .setBranchId(TestConstants.yggdrash())
                .setWallet(TestConstants.wallet())
                .buildTransaction();
        log.debug("txSize : {} , {} ",  tx1.toString().getBytes().length, tx2.toString().getBytes().length);

        assert tx1.toString().getBytes().length > tx2.toString().getBytes().length;
    }

}
