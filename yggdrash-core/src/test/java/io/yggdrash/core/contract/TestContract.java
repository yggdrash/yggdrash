package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractChannelMethod;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.channel.ContractChannel;
import io.yggdrash.contract.core.channel.ContractMethodType;
import io.yggdrash.contract.core.store.ReadWriterStore;
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


    @ContractReceipt
    private Receipt txReceipt;

    public void init(StateStore stateStore) {
    }

    @InvokeTransaction
    public Boolean doNothing(JsonObject params) {
        // pass
        return true;
    }

    @InvokeTransaction
    public Receipt transfer(JsonObject params) {

        String to = params.get("to").getAsString().toLowerCase();
        BigInteger amount = params.get("amount").getAsBigInteger();
        return txReceipt;
    }

    protected boolean transfer(String from, String to, BigInteger amount, BigInteger fee) {
        log.debug("Transfer from {} to {} value {} fee {} ", from, to, amount, fee);
        return true;

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

    public void callContractChannelInvoke(String contract, String method) {
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

    public void callContractChnnelMethod(String contract, String method) {
        JsonObject param = new JsonObject();
        param.addProperty("from", "FROM_ACCOUNT");
        param.addProperty("to", "TO_ACCOUNT");
        param.addProperty("amount", BigInteger.valueOf(100L));
        param.addProperty("fee", BigInteger.valueOf(1L));

        JsonObject ressult = channel.call(contract, ContractMethodType.CHANNEL_METHOD, method,  param);


    }

    @ContractChannelMethod
    public boolean transferChannel(JsonObject params) {
        //String contractVersion = txReceipt.getContractVersion();


        String from = params.get("from").getAsString();
        String to = params.get("to").getAsString();
        BigInteger amount = params.get("amount").getAsBigInteger();
        BigInteger fee = params.get("fee").getAsBigInteger();

        return transfer(from, to, amount, fee);
    }
}
