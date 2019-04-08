package io.yggdrash.contract.yeed.intertransfer;

import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.contract.yeed.ehtereum.Eth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class Ether {
    ReadWriterStore<String, JsonObject> store;


    Ether(ReadWriterStore<String, JsonObject> store) {
        this.store = store;
    }

    // Step 1
    // escrow YEED ,Account A want to ETH,and Account B want to YEED
    public byte[] issueInterTransfer(byte[] txid, byte[] issuer, Integer chainId, byte[] sender, BigInteger eth, BigInteger yeed, byte[] data) {
        // Issure stake this interTranfer Proposal ID

        // issuer and Sender
        // Proposal ID
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(txid);
            baos.write(Ints.toByteArray(chainId));
            baos.write(issuer);
            baos.write(sender);
            baos.write(eth.toByteArray());
            // Stake YEED
            baos.write(yeed.toByteArray());
            // data is option
            baos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[]{};
        }


        byte[] proposalData = baos.toByteArray();
        // 32byte proposal ID
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
