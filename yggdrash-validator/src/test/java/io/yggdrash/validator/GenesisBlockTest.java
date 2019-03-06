package io.yggdrash.validator;

import io.yggdrash.common.utils.FileUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class GenesisBlockTest {

    private static final Logger log = LoggerFactory.getLogger(GenesisBlockTest.class);

    private GenesisBlock genesisBlock;

    @Before
    public void setUp() throws Exception {
        this.genesisBlock = new GenesisBlock();
    }

    @Test
    public void generateGenesisBlock() {
        this.genesisBlock.generateGenesisBlockFile();

        ClassLoader classLoader = getClass().getClassLoader();

        File genesisFile;
        String genesisString = null;

        try {
            genesisFile = new File(classLoader.getResource("./genesis/genesis.json").getFile());
            genesisString = FileUtil.readFileToString(genesisFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            assert false;
        }

        log.debug(genesisString);
    }

}