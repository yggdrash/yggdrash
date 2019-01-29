/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.gson.JsonArray;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import static io.yggdrash.TestConstants.TRANSFER_TO;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.core.wallet.Account;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;
import java.io.IOException;
import java.security.SignatureException;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

public class TransactionHuskTest  extends TestConstants.SlowTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionHuskTest.class);

    @Test
    public void shouldBeNotEquals() {
        TransactionHusk tx1 = createTransferTx();
        TransactionHusk tx2 = createTransferTx();
        assertThat(tx1).isNotEqualTo(tx2);
    }

    @Test
    public void transactionTest() {
        TransactionHusk tx1 = createTransferTx();
        assertThat(tx1).isNotNull();
        assertThat(tx1.getHash()).isNotNull();
    }

    @Test
    public void deserializeTransactionFromProtoTest() {
        TransactionHusk tx1 = createTransferTx();
        Proto.Transaction protoTx = tx1.getInstance();
        TransactionHusk deserializeTx = new TransactionHusk(protoTx);
        assertThat(tx1.getHash()).isEqualTo(deserializeTx.getHash());
    }

    @Test
    public void testGetAddressWithWallet() {
        TransactionHusk tx1 = createTransferTx();
        TransactionHusk tx2 = createTransferTx();

        assertThat(tx1.getAddress()).isEqualTo(tx2.getAddress());

        Wallet wallet = TestConstants.wallet();
        assertThat(wallet.getAddress()).isEqualTo(tx1.getAddress().getBytes());
    }

    @Test
    public void testGetAddressWithWalletAccount() throws IOException, InvalidCipherTextException {
        Account account = new Account();
        log.debug("Account: " + account.toString());
        log.debug("Account.address: " + Hex.toHexString(account.getAddress()));

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet: " + wallet.toString());
        log.debug("Wallet.address: " + Hex.toHexString(wallet.getAddress()));

        TransactionHusk tx1 = createTx(wallet);
        TransactionHusk tx2 = createTx(wallet);

        log.debug("Test Transaction1: " + tx1.toString());
        log.debug("Test Transaction1 Address: " + tx1.getAddress());

        log.debug("Test Transaction2: " + tx2.toString());
        log.debug("Test Transaction2 Address: " + tx2.getAddress());


        assertThat(wallet.getAddress()).isEqualTo(account.getAddress());
        assertThat(tx1.getAddress()).isEqualTo(tx2.getAddress());
        assertThat(account.getAddress()).isEqualTo(tx1.getAddress().getBytes());
    }

    @Test
    public void testGetAddressWithSig()
            throws IOException, InvalidCipherTextException, SignatureException {
        Account account = new Account();
        log.debug("Account: " + account.toString());
        log.debug("Account.address: " + Hex.toHexString(account.getAddress()));
        log.debug("Account.pubKey: " + Hex.toHexString(account.getKey().getPubKey()));

        Wallet wallet = new Wallet(account.getKey(), "tmp/path", "nodePri.key", "Aa1234567890!");
        log.debug("Wallet: " + wallet.toString());
        log.debug("Wallet.address: " + Hex.toHexString(wallet.getAddress()));
        log.debug("Wallet.pubKey: " + Hex.toHexString(wallet.getPubicKey()));

        TransactionHusk txHusk1 = createTx(wallet);
        log.debug("Test Transaction1: " + txHusk1.toString());
        log.debug("Test Transaction1 Address: " + txHusk1.getAddress());

        assertThat(txHusk1.verify()).isTrue();
        assertThat(wallet.getAddress()).isEqualTo(account.getAddress());
        assertThat(wallet.getAddress()).isEqualTo(txHusk1.getAddress().getBytes());

        byte[] hashedRawData = txHusk1.getHashForSigning().getBytes();
        log.debug("hashedRawData: " + Hex.toHexString(hashedRawData));

        byte[] signatureBin = txHusk1.getInstance().getSignature().toByteArray();
        log.debug("signatureBin: " + Hex.toHexString(signatureBin));

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signatureBin);
        ECKey key = ECKey.signatureToKey(hashedRawData, ecdsaSignature);

        byte[] address = key.getAddress();
        byte[] pubKey = key.getPubKey();

        log.debug("address: " + Hex.toHexString(address));
        log.debug("pubKey: " + Hex.toHexString(pubKey));

        assertThat(account.getAddress()).isEqualTo(address);
        assertThat(account.getKey().getPubKey()).isEqualTo(pubKey);
    }

    @Test
    public void shouldBeSignedTransaction() {
        TransactionHusk tx1 = createTransferTx();
        tx1.sign(TestConstants.wallet());

        assertThat(tx1.isSigned()).isTrue();
        assertThat(tx1.verify()).isTrue();
    }

    private TransactionHusk createTransferTx() {
        return BlockChainTestUtils.createTransferTxHusk();
    }

    private TransactionHusk createTx(Wallet wallet) {
        JsonArray txBody = ContractTestUtils.transferTxBodyJson(TRANSFER_TO, 100);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setWallet(wallet)
                .setBranchId(TestConstants.YEED)
                .addTransaction(txBody)
                .build();
    }
}
