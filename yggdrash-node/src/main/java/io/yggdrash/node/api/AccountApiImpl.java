package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.core.Account;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Runtime;
import io.yggdrash.core.exception.NonExistObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@AutoJsonRpcServiceImpl
public class AccountApiImpl implements AccountApi {

    private final ArrayList<String> addresses = new ArrayList<>();
    private final long balance = 100000;
    private final NodeManager nodeManager;
    private final Runtime runtime;
    private CoinContract coinContract;

    @Autowired
    public AccountApiImpl(NodeManager nodeManager, Runtime runtime) {
        this.nodeManager = nodeManager;
        this.runtime = runtime;
        this.coinContract = new CoinContract();
    }

    @Override
    public String createAccount() {
        try {
            Account account = new Account();
            AccountDto response = AccountDto.createBy(account);
            return response.getAddress();
        } catch (Exception exception) {
            throw new NonExistObjectException("account");
        }
    }

    @Override
    public ArrayList<String> accounts() {
        try {
            String addr1 = "0xA6cf59D72cB6c253b3CFe10d498aC8615453689B";
            String addr2 = "0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A";
            String addr3 = "0x1662E2457A0e079B03214dc3D5009bA2137006C7";
            addresses.add(addr1);
            addresses.add(addr2);
            addresses.add(addr3);

            return addresses;
        } catch (Exception exception) {
            throw new NonExistObjectException("accounts");
        }
    }

    @Override
    public String balanceOf(String data) throws Exception {
        JsonParser jsonParser = new JsonParser();
        JsonObject query = (JsonObject) jsonParser.parse(data);
        return runtime.query(coinContract, query).toString();
    }

    @Override
    public long getBalance(String address, int blockNumber) {
        try {
            return balance;
        } catch (Exception exception) {
            throw new NonExistObjectException("address or blockNumber");
        }
    }

    @Override
    public long getBalance(String address, String tag) {
        try {
            return balance;
        } catch (Exception exception) {
            throw new NonExistObjectException("address or tag");
        }
    }
}
