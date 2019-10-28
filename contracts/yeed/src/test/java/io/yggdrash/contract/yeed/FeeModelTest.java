package io.yggdrash.contract.yeed;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;

public class FeeModelTest {

    private static final Logger log = LoggerFactory.getLogger(FeeModelTest.class);

    @Test
    public void baseFeeModelTest() {
        BigInteger baseFee = BigInteger.TEN.pow(10);
        BigInteger oneYeed = BigInteger.TEN.pow(18); // 1 * 10 ^ 18 (decimal = 18)

        int txLength = 154; // 19-08-16 default size (but add fee is more)

        BigInteger networkFee = baseFee.multiply(BigInteger.valueOf(txLength));

        printBigInteger(networkFee);

        txLength = 200; // Tx size default value
        BigInteger targetFee = BigInteger.TEN.pow(16); // 0.01 YEED

        // Base fee * txLength = targetFee -> Base Fee = targetFee / txLength
        BigInteger sendYeed = oneYeed;
        BigInteger calBaseFee = targetFee.divide(BigInteger.valueOf(txLength));

        printBigInteger(sendYeed);
        printBigInteger(calBaseFee);

        BigInteger baseFeeCal = new BigInteger("50000000000000");
        BigInteger checkLong = BigInteger.valueOf(50000000000000L);
        Assert.assertTrue("Check equals Long and String", checkLong.compareTo(baseFeeCal) == 0);
        BigInteger calFee = baseFeeCal.multiply(BigInteger.valueOf(txLength));

        BigInteger totalSend = sendYeed.add(calFee);

        BigDecimal totalSendDecimal = new BigDecimal(totalSend);
        BigDecimal oneYeedDecimal = new BigDecimal(oneYeed);
        log.debug(totalSendDecimal.divide(oneYeedDecimal).toString());


    }

    @Test
    public void baseCurrencyModelSpeedTest() {
        BigInteger oneYeed = BigInteger.TEN.pow(18); // 1 * 10 ^ 18 (decimal = 18)
        for (int i = 0; i < 1000000; i++) {
            BigInteger bi = oneYeed.multiply(BigInteger.valueOf(i));
            //log.debug(Base64.getEncoder().encodeToString(bi.toByteArray()));
        }

    }

    private void printBigInteger(BigInteger value) {
        String bigIntegerString = value.toString();
        String base64String = Base64.getEncoder().encodeToString(value.toByteArray());
        log.debug("{} Byte, String => {} : length {} , Base64 => {} : length {}, Save {} byte({})",
                value.toByteArray().length,
                bigIntegerString, bigIntegerString.length(),
                base64String, base64String.length(),
                bigIntegerString.length() - base64String.length(),
                1 - (bigIntegerString.length() - base64String.length()) * 1.0f / bigIntegerString.length()
        );
    }
}
