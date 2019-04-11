package io.yggdrash.contract.yeed.propose;

import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.contract.yeed.ehtereum.EthTransaction;
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
        ProposeType proposeType = ProposeType.ETHER_TO_YEED;

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
        ProposeInterChain recover = new ProposeInterChain(testPropose.toJsonObject());
        assert recover.getProposeId().equals(testPropose.getProposeId());
    }

    @Test
    public void checkProposeTypeAndEther() {
        String ethRawTransaction = "0xf86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethRawTransaction);

        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);

        String transactionId = "0x00";
        String receiveAddress = HexUtil.toHexString(ethTransaction.getReceiveAddress());
        BigInteger receiveEth = ethTransaction.getValue();
        Integer receiveChainId = ethTransaction.getChainId();
        ProposeType proposeType = ProposeType.ETHER_TO_YEED;

        String senderAddress = "c3cf7a283a4415ce3c41f5374934612389334780";

        String inputData = null;
        BigInteger stakeYeed = new BigInteger("1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("100000000000000000");

        String issuer = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";

        ProposeInterChain ethPropose = new ProposeInterChain(transactionId, receiveAddress,
                receiveEth, receiveChainId, proposeType, senderAddress, inputData, stakeYeed,
                targetBlockHeight, fee, issuer);

        assert ethPropose.getFee().compareTo(fee) == 0;

        log.debug("Propose ETH : {}", ethPropose.getReceiveAsset());
        log.debug("Ethereum Transaction ETH : {}", ethTransaction.getValue());

        assert ethPropose.getReceiveAsset().compareTo(ethTransaction.getValue()) == 0;
        assert ethPropose.getReceiveAddress().equals(HexUtil.toHexString(ethTransaction.getReceiveAddress()));

        log.debug("Ethereum transaction ID : 0x{} ", HexUtil.toHexString(ethTransaction.getTxHash()));


        log.debug("propose ID : {}", ethPropose.getProposeId());

        log.debug("Propose Stake YEED : {}", ethPropose.getStakeYeed());

    }



}