package io.yggdrash.contract.yeed;

import com.google.gson.JsonObject;
import io.yggdrash.contract.core.store.ReadWriterStore;
import java.math.BigInteger;

public class InterTransferEth {
    ReadWriterStore<String, JsonObject> store;


    InterTransferEth(ReadWriterStore<String, JsonObject> store) {
        this.store = store;
    }



    // Step 1
    // escrow YEED ,Account A want to ETH,and Account B want to YEED
    public String issueInterTransfer(byte[] txid, byte[] issuer, byte[] sender, BigInteger eth,BigInteger yeed,byte[] data) {
        return "";
    }





    // Step 2
    //

}
