package io.yggdrash.core;

import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.SerializeUtils;

import java.io.ByteArrayOutputStream;

public class TransactionValidator {

    public Boolean txSigValidate(Transaction tx) {
        byte[] bin = SerializeUtils.serialize(tx.getData());

        ByteArrayOutputStream tmpTx = new ByteArrayOutputStream();
        try {
            tmpTx.write(tx.getHeader().getType());
            tmpTx.write(tx.getHeader().getVersion());
            tmpTx.write(HashUtil.sha3(bin));
            tmpTx.write(ByteUtil.longToBytes(bin.length));
            tmpTx.write(ByteUtil.longToBytes(tx.getHeader().getTimestamp()));

            byte[] tmpSignDataHash = HashUtil.sha3(tmpTx.toByteArray());

            ECKey.ECDSASignature sig = new ECKey.ECDSASignature(tx.getHeader().getSignature());
            ECKey keyFromSig = ECKey.signatureToKey(tx.getHeader().getDataHashForSigning(), sig);

            return keyFromSig.verify(tmpSignDataHash, sig);

        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public void txFormatValidate() {
        // todo transaction format validation
    }
}
