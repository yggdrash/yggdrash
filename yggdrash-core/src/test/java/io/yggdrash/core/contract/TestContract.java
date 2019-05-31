package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.channel.ContractMethodType;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.osgi.ContractManagerBuilderTest;
import io.yggdrash.core.runtime.annotation.YggdrashContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

@YggdrashContract
public class TestContract implements Contract {

    private static final Logger log = LoggerFactory.getLogger(TestContract.class);

    @ContractChannelField
    public ContractChannel channel;

    @ContractStateStore
    private ReadWriterStore<String, JsonObject> state;


    @ContractTransactionReceipt
    private TransactionReceipt txReceipt;

    public void init(StateStore stateStore) {
    }

    @InvokeTransaction
    public Boolean doNothing(JsonObject params) {
        // pass
        return true;
    }

    @InvokeTransaction
    public TransactionReceipt transfer(JsonObject params) {

        String to = params.get("to").getAsString().toLowerCase();
        BigInteger amount = params.get("amount").getAsBigInteger();
        return txReceipt;
    }

    @ContractQuery
    public String someQuery() {
        return "";
    }

    @ContractQuery
    public String yesmanQuery(JsonObject param) {
        log.debug("yesmanQuery : {}", param.toString());
        return "YES";
    }

    @InvokeTransaction
    public void callMethod(JsonObject param) {
        log.debug("callMethod : {}", param.toString());
    }

    public void callContractChannel(String contract, String method) {
        JsonObject param = new JsonObject();
        param.addProperty("contract", contract);
        param.addProperty("method", method);
        JsonObject object = channel.call(contract, ContractMethodType.INVOKE, method,  param);
        if (object != null) {
            log.debug(object.toString());
        }
    }

    public void callContractChannelQuery(String contract, String method) {
        JsonObject param = new JsonObject();
        param.addProperty("contract", contract);
        param.addProperty("method", method);
        JsonObject ressult = channel.call(contract, ContractMethodType.QUERY, method,  param);
        log.debug(ressult.toString());

    }


}
