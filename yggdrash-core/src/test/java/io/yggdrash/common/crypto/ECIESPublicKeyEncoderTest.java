package io.yggdrash.common.crypto;

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.util.encoders.Hex;

import static io.yggdrash.common.crypto.ECKey.CURVE;

public class ECIESPublicKeyEncoderTest {
    private final String pubString = "040947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad75aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b";
    private final byte[] pubKey = Hex.decode(pubString);

    @Test
    public void getEncoded() {
        ECPublicKeyParameters param = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pubKey), CURVE);
        ECIESPublicKeyEncoder encoder = new ECIESPublicKeyEncoder();
        Assert.assertNotNull(encoder.getEncoded(param));
    }
}