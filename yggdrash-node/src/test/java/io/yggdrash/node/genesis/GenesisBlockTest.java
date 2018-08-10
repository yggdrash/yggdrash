package io.yggdrash.node.genesis;

import org.junit.Before;
import org.junit.Test;

public class GenesisBlockTest {

    private GenesisBlock genesisBlock;

    @Before
    public void setUp() throws Exception {
        this.genesisBlock = new GenesisBlock();

    }

    @Test
    public void generateGenesisBlock() {

        try {
            this.genesisBlock.generateGenesisBlockFile();
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

}
