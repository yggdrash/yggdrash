package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(JsonRpcConfig.class)
public class BlockApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Autowired
    JsonRpcHttpClient jsonRpcHttpClient;
    BlockApiImpl blockApi = new BlockApiImpl();

    @Test
    public void blockNumberTest() {
        try {
            BlockApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    BlockApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.blockNumber()).isZero();
        } catch (Exception exception) {
            log.debug("blockNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByHashTest() {
        try {
            BlockApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    BlockApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getBlockByHash("0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A",
                    "latest")).isNotEmpty();
        } catch (Exception exception) {
            log.debug("getBlockByHashTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByNumberTest() {
        try {
            BlockApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    BlockApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.getBlockByNumber("0xbbF5029Fd710d227630c8b7d338051B8E76d50B3",
                    true)).isNotEmpty();
        } catch (Exception exception) {
            log.debug("getBlockByNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void newBlockFilter() {
        try {
            BlockApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    BlockApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.newBlockFilter()).isZero();
        } catch (Exception exception) {
            log.debug("newBlockFilter :: exception : " + exception);
        }
    }

}
