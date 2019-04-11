package io.yggdrash.contract.yeed.intertransfer;

import java.math.BigInteger;

public class TransactionConfirm {
    String transactionConfirmId;
    String proposeId;
    String valifiedTransactionId;

    long networkBlockheight;

    String sendAddress;
    BigInteger transferBalance;
}
