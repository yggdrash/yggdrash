package io.yggdrash.core.wallet;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class AddressTest {
    private static final String ADDRESS_HEX = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final byte[] ADDRESS_BYTES = Hex.decode(ADDRESS_HEX);

    @Test
    public void equalsTest() {
        assertThat(Address.of("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94")).isEqualTo(Address.of(ADDRESS_HEX));
        assertThat(Address.of("00")).isNotEqualTo(Address.of(ADDRESS_HEX));
    }

    @Test
    public void toStringTest() {
        assertThat(Address.of(ADDRESS_HEX).toString()).isEqualTo(ADDRESS_HEX);
    }

    @Test
    public void hashCodeTest() {
        assertThat(Address.of(ADDRESS_HEX).hashCode()).isEqualTo(Arrays.hashCode(ADDRESS_BYTES));
    }

    @Test
    public void getBytesTest() {
        assertThat(Address.of(ADDRESS_HEX).getBytes()).isEqualTo(ADDRESS_BYTES);
    }
}