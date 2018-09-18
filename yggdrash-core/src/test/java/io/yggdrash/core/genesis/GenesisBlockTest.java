package io.yggdrash.core.genesis;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

public class GenesisBlockTest {

    private static final Logger log = LoggerFactory.getLogger(GenesisBlockTest.class);

    @Test
    public void generateGenesisBlock() throws InvalidCipherTextException, IOException {
        log.debug(System.lineSeparator() + new GenesisBlock().getGenesisJson());
    }

}
