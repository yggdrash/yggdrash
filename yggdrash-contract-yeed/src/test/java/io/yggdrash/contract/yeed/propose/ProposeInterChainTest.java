package io.yggdrash.contract.yeed.propose;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class ProposeInterChainTest {

    private static final Logger log = LoggerFactory.getLogger(ProposeInterChainTest.class);

    @Test
    public void proposeInterChainGenerateIdTest() {
        String transactionId = "0x00";
        String receiveAddress = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
        BigInteger receiveEth = new BigInteger("10000000");
        int receiveChainId = 1;
        ProposeType proposeType = ProposeType.ETHER;

        String senderAddress = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";

        String inputData = null;
        BigInteger stakeYeed = new BigInteger("1000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("1000000000");
        String issuer = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";


        ProposeInterChain testPropose = new ProposeInterChain(transactionId, receiveAddress,
                receiveEth, receiveChainId, proposeType, senderAddress, inputData, stakeYeed,
                targetBlockHeight, fee, issuer);

        log.debug("Propose ID : {} ", testPropose.getProposeId());
        assertNotNull(testPropose.getProposeId());

        targetBlockHeight = 10000000L;
        ProposeInterChain testPropose2 = new ProposeInterChain(transactionId, receiveAddress,
                receiveEth, receiveChainId, proposeType, senderAddress, inputData, stakeYeed,
                targetBlockHeight, fee, issuer);

        assertNotEquals(testPropose.getProposeId(), testPropose2.getProposeId());



    }

}