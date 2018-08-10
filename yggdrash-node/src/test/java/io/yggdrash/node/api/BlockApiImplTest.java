package io.yggdrash.node.api;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);
    private static final BlockApi blockApi = new JsonRpcConfig().blockApi();

    @Test
    public void blockApiIsNotNull() {
        assertThat(blockApi).isNotNull();
    }

    @Test
    public void blockNumberTest() {
        try {
            assertThat(blockApi.blockNumber()).isNotNull();
        } catch (Exception exception) {
            log.debug("blockNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void getAllBlockTest() {
        try {
            assertThat(blockApi.getAllBlock().size()).isNotZero();
        } catch (Exception exception) {
            log.debug("getAllBlockTest :: exception : " + exception, exception);
        }
    }

    @Test
    public void getBlockByHashTest() {
        try {
            String hashOfBlock = blockApi.getBlockByHash("0", true).getPrevBlockHash();
            assertThat(blockApi.getBlockByHash(hashOfBlock, true)).isNotNull();
        } catch (Exception exception) {
            log.debug("getBlockByHashTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByNumberTest() {
        try {
            assertThat(blockApi.getBlockByNumber("0", true)).isNotNull();
        } catch (Exception exception) {
            log.debug("getBlockByNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void newBlockFilter() {
        try {
            assertThat(blockApi.newBlockFilter()).isZero();
        } catch (Exception exception) {
            log.debug("newBlockFilter :: exception : " + exception);
        }
    }

}
