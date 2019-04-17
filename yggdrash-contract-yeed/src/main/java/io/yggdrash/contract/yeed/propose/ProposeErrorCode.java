package io.yggdrash.contract.yeed.propose;

import java.util.ArrayList;
import java.util.List;

public enum ProposeErrorCode {
    PROPOSE_VALID(0),                       // 000
    PROPOSE_RECEIVE_ADDRESS_INVALID(1),     // 001
    PROPOSE_SENDER_ADDRESS_INVALID(2),      // 100
    PROPOSE_RECEIVE_CHAIN_ID_INVALID(4);    // 010


    private int code;

    ProposeErrorCode(int code) {
        this.code = code;
    }

    public static int addCode(boolean flag, ProposeErrorCode appendCode) {
        // flash is false
        if (!flag) {
            return appendCode.code;
        }
        return PROPOSE_VALID.code;
    }

    public static List<String> errorLogs(int code) {
        List<String> errorString = new ArrayList<>();
        if ((code & PROPOSE_RECEIVE_ADDRESS_INVALID.code) == PROPOSE_RECEIVE_ADDRESS_INVALID.code) {
            errorString.add("Receive Address is Invalid");
        }
        if ((code & PROPOSE_SENDER_ADDRESS_INVALID.code) == PROPOSE_SENDER_ADDRESS_INVALID.code) {
            errorString.add("Sender Address is Invalid");
        }
        if ((code & PROPOSE_RECEIVE_CHAIN_ID_INVALID.code) == PROPOSE_RECEIVE_CHAIN_ID_INVALID.code) {
            errorString.add("Receive CHAIN ID is Invalid");
        }
        return errorString;




    }


}
