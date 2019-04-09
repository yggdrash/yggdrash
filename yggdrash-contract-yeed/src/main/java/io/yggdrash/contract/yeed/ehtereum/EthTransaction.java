package io.yggdrash.contract.yeed.ehtereum;


import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.security.SignatureException;



public class EthTransaction {
    byte[] txHash;
    byte[] nonce;
    byte[] gasPrice;
    byte[] gasLimit;
    byte[] receiveAddress;
    byte[] value;
    byte[] data;

    public byte[] getTxHash() {
        return txHash;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public byte[] getGasPrice() {
        return gasPrice;
    }

    public byte[] getGasLimit() {
        return gasLimit;
    }

    public byte[] getReceiveAddress() {
        return receiveAddress;
    }

    public byte[] getValue() {
        return value;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getSendAddress() {
        return sendAddress;
    }

    public Integer getChainId() {
        return chainId;
    }

    byte[] sendAddress;

    Integer chainId = -1;

    private static final Logger log = LoggerFactory.getLogger(EthTransaction.class);
    private static final int CHAIN_ID_INC = 35;
    private static final int LOWER_REAL_V = 27;


    public EthTransaction(byte[] rawTransaction) {
        // check rawTransaction
        RLPList decodedTxList = RLP.decode2(rawTransaction);
        RLPList ethTx = (RLPList) decodedTxList.get(0);

        if (ethTx.size() != 8) {
            // not Ethereum Transction
        }

        nonce = ethTx.get(0).getRLPData();
        gasPrice = ethTx.get(1).getRLPData();
        gasLimit = ethTx.get(2).getRLPData();
        receiveAddress = ethTx.get(3).getRLPData();
        value = ethTx.get(4).getRLPData();
        data = ethTx.get(5).getRLPData();

        ECKey.ECDSASignature signature = null;
        // only parse signature in case tx is signed
        if (ethTx.get(6).getRLPData() != null) {
            byte[] vData =  ethTx.get(6).getRLPData();
            BigInteger v = ByteUtil.bytesToBigInteger(vData);
            byte[] r = ethTx.get(7).getRLPData();
            byte[] s = ethTx.get(8).getRLPData();
            chainId = extractChainIdFromRawSignature(v, r, s);
            if (r != null && s != null) {
                signature = ECKey.ECDSASignature.fromComponents(r, s, getRealV(v));
            }
        } else {
            log.debug("RLP encoded tx is not signed!");
        }
        txHash = HashUtil.sha3(rawTransaction);
        log.debug(HexUtil.toHexString(txHash));

        try {
            sendAddress = ECKey.signatureToAddress(txHash, signature);
        } catch (SignatureException e) {
            sendAddress = new byte[0];
        }
    }

    private Integer extractChainIdFromRawSignature(BigInteger bv, byte[] r, byte[] s) {
        if (r == null && s == null) {
            return bv.intValue();  // EIP 86
        }
        if (bv.bitLength() > 31) {
            return Integer.MAX_VALUE; // chainId is limited to 31 bits, longer are not valid for now
        }
        long v = bv.longValue();
        log.debug("{} V", v);
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return null;
        return (int) ((v - CHAIN_ID_INC) / 2);
    }


    private byte getRealV(BigInteger bv) {
        if (bv.bitLength() > 31) return 0; // chainId is limited to 31 bits, longer are not valid for now
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return (byte) v;
        byte realV = LOWER_REAL_V;
        int inc = 0;
        if ((int) v % 2 == 0) {
            inc = 1;
        }
        return (byte) (realV + inc);
    }


}