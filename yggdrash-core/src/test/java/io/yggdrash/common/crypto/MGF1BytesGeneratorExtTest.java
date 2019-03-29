package io.yggdrash.common.crypto;

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.crypto.DerivationParameters;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.params.MGFParameters;
import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;

public class MGF1BytesGeneratorExtTest {

    @Test
    public void getDigest() {
        MGF1BytesGeneratorExt ext = new MGF1BytesGeneratorExt(new SHA1Digest(), 1);
        byte[] VZ = BigIntegers.asUnsignedByteArray(1, BigInteger.ONE);

        DerivationParameters kdfParam = new MGFParameters(VZ);

        ext.init(kdfParam);

        byte[] K1 = new byte[4];
        assert ext.generateBytes(K1, 0, K1.length) > 0;

        Assert.assertNotNull(ext.getDigest());
    }
}