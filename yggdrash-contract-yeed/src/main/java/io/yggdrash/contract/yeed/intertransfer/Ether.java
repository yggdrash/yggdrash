package io.yggdrash.contract.yeed.intertransfer;

import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.yeed.ehtereum.EthTransaction;
import io.yggdrash.contract.yeed.propose.ProposeInterChain;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class Ether {

    // Step 1
    // escrow YEED ,Account A want to ETH,and Account B want to YEED
    // expire block height
    //
    public byte[] issueInterTransfer(byte[] transactionId, byte[] issuer, Integer chainId, byte[] sender, BigInteger eth, BigInteger yeed, byte[] data) {
        // Issue stake this interTransfer Proposal ID

        // issuer and Sender
        // Proposal ID
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] proposalData;
        try {
            baos.write(transactionId);
            baos.write(Ints.toByteArray(chainId));
            baos.write(issuer);
            baos.write(sender);
            baos.write(eth.toByteArray());
            // Stake YEED
            baos.write(yeed.toByteArray());
            // data is option
            baos.write(data);
            proposalData = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[]{};
        }

        // 32byte proposal ID
        byte[] proposalID = HashUtil.sha3(proposalData);
        // add Event proposal ID created

        // Check YEED to issuer

        // Stake YEED to proposal ID

        return proposalID;
    }

    public EthTransaction parseEthRawTransaction(String rawTransactionData) {
        byte[] rawTransaction = HexUtil.hexStringToBytes(rawTransactionData);

        return new EthTransaction(rawTransaction);
    }

    public boolean checkProposalTransaction(ProposeInterChain propose, EthTransaction ethTransactionTx) {
        // get ProposalData

        // check ethTransactionTx
        if (ethTransactionTx.getChainId() != 1) {
            // This Network is not ETH
            return false;
        }

        // get Proposal Data

        return true;
    }
    // Step 2
    //

}
