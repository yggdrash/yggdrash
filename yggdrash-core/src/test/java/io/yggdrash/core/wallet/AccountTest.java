package io.yggdrash.core.wallet;

import io.yggdrash.common.crypto.ECKey;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AccountTest {

    @Test
    public void test() {
        Account account = new Account();
        ECKey key = account.getKey();
        assertThat(account.getAddress()).isEqualTo(account.getAddress());
        assertThat(account.toString()).contains(Hex.toHexString(key.getPubKey()));
    }
}