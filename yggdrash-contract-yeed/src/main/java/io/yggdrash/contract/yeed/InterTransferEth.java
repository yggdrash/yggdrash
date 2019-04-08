package io.yggdrash.contract.yeed;

import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.yeed.ehtereum.Eth;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class InterTransferEth {
    ReadWriterStore<String, JsonObject> store;


    InterTransferEth(ReadWriterStore<String, JsonObject> store) {
        this.store = store;
    }



    // Step 1
    // escrow YEED ,Account A want to ETH,and Account B want to YEED
    public byte[] issueInterTransfer(byte[] txid, byte[] issuer, byte[] sender, BigInteger eth, BigInteger yeed, byte[] data) {
        // Proposal ID
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.writeBytes(txid);
        baos.writeBytes(issuer);
        baos.writeBytes(sender);
        baos.writeBytes(eth.toByteArray());
        baos.writeBytes(yeed.toByteArray());
        baos.writeBytes(data);

        byte[] proposalData = baos.toByteArray();
        byte[] proposalID = HashUtil.sha3(proposalData);
        // add Event proposal ID created
        
        // Check YEED to issuer

        // Stake YEED to proposal ID

        return proposalID;
    }

    public Eth parseEthRawTransaction(byte[] rawTransction) {

        return new Eth(rawTransction);
    }

    public boolean checkProposalTransaction(byte[] proposalId, Eth ethTx) {
        // get ProposalData

        // check ethTx
        if (ethTx.getChainId() != 1) {
            // This Network is not ETH
            return false;
        }

        // get Proposal Data

        return true;
    }
    // Step 2
    //

}
