package io.yggdrash.core.genesis;

import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.genesis.GenesisBlock;
import io.yggdrash.util.FileUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

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

            ClassLoader classLoader = getClass().getClassLoader();
            File genesisFile = new File(classLoader.getResource(
                    new DefaultConfig().getConfig().getString("genesis.block")).getFile());
            String genesisString = FileUtil.readFileToString(genesisFile);

            System.out.println(genesisString);

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

}
