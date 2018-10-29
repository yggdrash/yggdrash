package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.TestUtils;
import io.yggdrash.core.Branch;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.account.Address;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.contract.ContractTx;
import io.yggdrash.node.controller.TransactionDto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class TransactionApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);
    private static final BlockApi blockApi = new JsonRpcConfig().blockApi();
    private static final TransactionApi txApi = new JsonRpcConfig().transactionApi();

    private final int blockNumber = 3;
    private final int txIndexPosition = 2;
    private final String branchId = BranchId.STEM;

    @Before
    public void setUp() {
        sendTransactionTest();
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
    public void getBlockTransactionCountByHashTest() {
        try {
            assertThat(txApi.getTransactionCountByBlockHash(branchId,
                    "d52fffa14f5b88b141d05d8e28c90d8131db1aa63e076bfea9c28c3060049e12"))
                    .isNotZero();
        } catch (Exception exception) {
            log.debug("\n\ngetBlockTransactionCountByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getBlockTransactionCountByNumberTest() {
        try {
            assertThat(txApi.getTransactionCountByBlockNumber(branchId, blockNumber)).isNotZero();
        } catch (Throwable exception) {
            log.debug("\n\ngetBlockTransactionCountByNumberTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByHashTest() {
        try {
            //TransactionHusk tx = TestUtils.createTxHusk();
            //txApi.sendTransaction(TransactionDto.createBy(tx));
            assertThat(txApi.getTransactionByHash(branchId,
                    "f5912fde84c6a3a44b4e529077ca9bf28feccd847137e44a77cd17e9fb9c1353"))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockHashTest() {
        try {
            assertThat(txApi.getTransactionByBlockHash(branchId,
                    "5ef71a90c6d99c7bc13bfbcaffb50cb89210678e99ed6626c9d2f378700b392c",
                    2)).isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByBlockHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockNumberTest() {
        try {
            assertThat(txApi.getTransactionByBlockNumber(branchId, blockNumber, txIndexPosition))
                    .isNotNull();
        } catch (Exception e) {
            log.debug("\n\ngetTransactionByBlockNumberTest :: exception => " + e);
        }
    }

    @Test
    public void getTransactionByBlockNumberWithTagTest() {
        try {
            String tag = "latest";
            txApi.getTransactionByBlockNumber(branchId, tag, txIndexPosition);
        } catch (Exception e) {
            log.debug("\n\ngetTransactionByBlockNumberWithTagTest :: exception => " + e);
        }
    }

    @Test
    public void checkTransactionJsonFormat() throws IOException {
        TransactionHusk tx = TestUtils.createTransferTxHusk();
        ObjectMapper objectMapper = TestUtils.getMapper();
        log.debug("\n\nTransaction Format : "
                + objectMapper.writeValueAsString(TransactionDto.createBy(tx)));
    }

    @Test
    public void sendTransactionTest() {
        Wallet wallet = TestUtils.wallet();
        TransactionHusk tx = ContractTx.createYeedTx(
                wallet, new Address(wallet.getAddress()), 100);

        // Request Transaction with jsonStr
        try {
            assertThat(txApi.sendTransaction(TransactionDto.createBy(tx))).isNotEmpty();
        } catch (Exception exception) {
            log.debug("\n\nsendTransactionTest :: exception => " + exception);
        }
    }

    @Test
    public void sendRawTransactionTest() {
        // Request Transaction with byteArr
        try {
            byte[] input = TestUtils.createTransferTxHusk().toBinary();
            // Convert byteArray to Transaction
            assertThat(txApi.sendRawTransaction(input)).isNotEmpty();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    @Test
    public void newPendingTransactionFilterTest() {
        try {
            assertThat(txApi.newPendingTransactionFilter(Branch.STEM)).isGreaterThanOrEqualTo(0);
            assertThat(txApi.newPendingTransactionFilter(Branch.YEED)).isGreaterThanOrEqualTo(0);
        } catch (Exception e) {
            log.debug("\n\nnewPendingTransactionFilterTest :: exception => " + e);
        }
    }

    @Test
    public void getAllTransactionReceiptTest() {
        try {
            assertThat(txApi.getAllTransactionReceipt(Branch.STEM)).isNotEmpty();
            assertThat(txApi.getAllTransactionReceipt(Branch.YEED)).isNotEmpty();
        } catch (Exception e) {
            log.debug("\n\ngetAllTransactionReceiptTest :: exception => " + e);
        }
    }

    @Test
    public void txSigValidateTest() throws IOException {
        // Create Transaction
        TransactionHusk tx = TestUtils.createTransferTxHusk();

        ObjectMapper mapper = TestUtils.getMapper();
        String jsonStr = mapper.writeValueAsString(TransactionDto.createBy(tx));

        // Receive Transaction
        TransactionDto resDto = mapper.readValue(jsonStr, TransactionDto.class);

        // Signature Validation
        assertTrue(TransactionDto.of(resDto).verify());
    }

}