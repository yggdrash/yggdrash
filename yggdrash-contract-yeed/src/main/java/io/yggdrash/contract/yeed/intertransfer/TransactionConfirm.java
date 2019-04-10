package io.yggdrash.contract.yeed.intertransfer;

import java.math.BigInteger;

public class TransactionConfirm {
    String proposeId;
    String transactionId;
    Integer chainId;

    long targetBlock;
    BigInteger transferBalance;
}
