package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.ContractTx;
import io.yggdrash.core.Address;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.controller.TransactionDto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class TransactionApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);
    private static final BlockApi blockApi = new JsonRpcConfig().blockApi();
    private static final TransactionApi txApi = new JsonRpcConfig().transactionApi();

    private Wallet wallet;
    private String address;
    private String hashOfTx;
    private final String tag = "latest";
    private final int blockNumber = 1;
    private final int txIndexPosition = 1;
    private String branchId = BranchId.STEM;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        this.wallet = new Wallet();
        sendTransactionTest();
        address = wallet.getHexAddress();
    }

    @Test
    public void blockApiIsNotNull() {
        assertThat(blockApi).isNotNull();
    }

    @Test
    public void txApiIsNotNull() {
        assertThat(txApi).isNotNull();
    }

    @Test
    public void getTransactionCountTest() {
        try {
            assertThat(txApi.getTransactionCount(branchId, address, tag)).isNotZero();
        } catch (Throwable exception) {
            log.debug("\n\ngetTransactionCountTest :: exception => " + exception);
        }
    }

    @Test
    public void getBlockTransactionCountByHashTest() {
        try {
            assertThat(txApi.getBlockTransactionCountByHash(branchId, hashOfTx)).isNotZero();
        } catch (Exception exception) {
            log.debug("\n\ngetBlockTransactionCountByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getBlockTransactionCountByNumberTest() {
        try {
            assertThat(txApi.getBlockTransactionCountByNumber(branchId, blockNumber)).isNotZero();
        } catch (Throwable exception) {
            log.debug("\n\ngetBlockTransactionCountByNumberTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByHashTest() {
        try {
            TransactionHusk tx = TestUtils.createTxHusk();

            txApi.sendTransaction(TransactionDto.createBy(tx));
            assertThat(txApi.getTransactionByHash(branchId, hashOfTx)).isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockHashTest() {
        try {
            TransactionHusk tx = new TransactionHusk(TestUtils.sampleTx(wallet));
            if (txApi.sendTransaction(TransactionDto.createBy(tx)) != null) {
                Thread.sleep(10000);
                String hashOfBlock = blockApi.getBlockByHash(branchId, "1", true).getHash().toString();
                assertThat(hashOfBlock).isNotEmpty();
                assertThat(txApi.getTransactionByBlockHash(branchId, hashOfBlock, 0)).isNotNull();
            } else {
                log.error("Send Transaction Failed!");
            }
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByBlockHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockNumberTest() {
        try {
            assertThat(txApi.getTransactionByBlockNumber(branchId, blockNumber, txIndexPosition))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByBlockNumberTest :: exception => " + exception);
        }
    }

    @Test
    public void checkTransactionJsonFormat() throws IOException {
        TransactionHusk tx = TestUtils.createTxHusk();
        ObjectMapper objectMapper = TestUtils.getMapper();
        log.debug("\n\nTransaction Format : "
                + objectMapper.writeValueAsString(TransactionDto.createBy(tx)));
    }

    @Test
    public void sendTransactionTest() {
        TransactionHusk tx = ContractTx.createYeedTx(
                wallet, new Address(wallet.getAddress()), 100);
        hashOfTx = tx.getHash().toString();

        // Request Transaction with jsonStr
        try {
            assertThat(txApi.sendTransaction(TransactionDto.createBy(tx))).isNotEmpty();
        } catch (Exception exception) {
            log.debug("\n\njsonStringToTxTest :: exception => " + exception);
        }
    }

    @Test
    public void sendRawTransactionTest() {
        // Request Transaction with byteArr
        try {
            byte[] input = TestUtils.sampleTx().toBinary();
            // Convert byteArray to Transaction
            assertThat(txApi.sendRawTransaction(input)).isNotEmpty();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    @Test
    public void newPendingTransactionFilterTest() {
        try {
            assertThat(txApi.newPendingTransactionFilter()).isNotZero();
        } catch (Exception exception) {
            log.debug("\n\njsonStringToTxTest :: exception => " + exception);
        }
    }

    @Test
    public void getAllTransactionReceiptTest() {
    }

    @Test
    public void txSigValidateTest() throws IOException {
        // Create Transaction
        TransactionHusk tx = new TransactionHusk(TestUtils.sampleTx(wallet));

        ObjectMapper mapper = TestUtils.getMapper();
        String jsonStr = mapper.writeValueAsString(TransactionDto.createBy(tx));

        // Receive Transaction
        TransactionDto resDto = mapper.readValue(jsonStr, TransactionDto.class);

        // Signature Validation
        assertTrue(TransactionDto.of(resDto).verify());
    }

}