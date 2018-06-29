package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.Account;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@AutoJsonRpcServiceImpl
public class AccountAPIImpl implements AccountAPI {

    private ArrayList<String> addresses = new ArrayList<>();
    private int balance = 100000;

    @Override
    public String createAccount() {
        Account account = new Account();
        AccountDto response = AccountDto.createBy(account);
        String address = response.getAddress();

        return address;
    }

    @Override
    public ArrayList<String> accounts() {
        String addr1 = "0xA6cf59D72cB6c253b3CFe10d498aC8615453689B";
        String addr2 = "0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A";
        String addr3 = "0x1662E2457A0e079B03214dc3D5009bA2137006C7";
        addresses.add(addr1);
        addresses.add(addr2);
        addresses.add(addr3);

        return addresses;
    }

    @Override
    public int getBalance(String address, int blockNumber) {
        return balance;
    }

    @Override
    public int getBalance(String address, String tag) {
        return balance;
    }
}

/*
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"createAccount","params":{}}' http://localhost:8080/account
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"accounts","params":{}}' http://localhost:8080/account
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getBalance","params":{"address":"0xA6cf59D72cB6c253b3CFe10d498aC8615453689B", "blockNumber":"1"}}' http://localhost:8080/account
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getBalance","params":{"address":"0xA6cf59D72cB6c253b3CFe10d498aC8615453689B", "tag":"latest"}}' http://localhost:8080/account
 */