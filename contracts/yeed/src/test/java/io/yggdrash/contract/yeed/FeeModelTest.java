package io.yggdrash.contract.yeed;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.util.Base64;

public class FeeModelTest {

    private static final Logger log = LoggerFactory.getLogger(FeeModelTest.class);

    @Test
    public void baseFeeModelTest() {
        BigInteger baseFee = BigInteger.TEN.pow(8);

        int txLength = 154; // 19-08-16 default size (but add fee is more)

        BigInteger networkFee = baseFee.multiply(BigInteger.valueOf(txLength));

        printBigInteger(networkFee);

        txLength = 200; // Tx size default value
        BigInteger oneYeed = BigInteger.TEN.pow(18); // 1 * 10 ^ 18 (decimal = 18)
        BigInteger targetFee = oneYeed.divide(BigInteger.TEN); // 0.1 YEED

        // Base fee * txLength = targetFee -> Base Fee = targetFee / txLength
        BigInteger sendYeed = oneYeed;
        BigInteger calBaseFee = targetFee.divide(BigInteger.valueOf(txLength));

        printBigInteger(sendYeed);
        printBigInteger(calBaseFee);


        BigInteger complexSend = oneYeed.multiply(BigInteger.valueOf(12345678901234L))
                .divide(BigInteger.valueOf(3));
        printBigInteger(complexSend);
        printBigInteger(calBaseFee);
    }

    @Test
    public void baseFeeModelSendTest() {

    }

    private void printBigInteger(BigInteger value) {
        String bigIntegerString = value.toString();
        log.debug("{} : length {}", bigIntegerString, bigIntegerString.length());
        String base64String = Base64.getEncoder().encodeToString(value.toByteArray());
        log.debug("{} : length {}", base64String, base64String.length());
        log.debug("Save Storage : {}", bigIntegerString.length() - base64String.length());
    }
}
