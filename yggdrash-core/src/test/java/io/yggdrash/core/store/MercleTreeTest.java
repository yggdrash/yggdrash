package io.yggdrash.core.store;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.crypto.Base64Util;
import io.yggdrash.common.util.BTreePrinterUtil;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.type.Account;
import io.yggdrash.core.type.Validator;
import org.iq80.leveldb.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MercleTreeTest {
    String storePath = "/tmp/data-store";

    @Before
    public void init() {
        FileUtils.deleteRecursively(new File(storePath));
    }

    /**
     * 각 노드 추가시 변경된 노드들의 Hash 값을 계산할때와 전체 노드를 후위순회 하여 계산할때의 Root Hash 값이 같은지를 검사한다.
     */
    @Test
    public void compareRootHashOfCommitAndRootHashOfPostOrder() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        accounts.add(new Account("51e5ae98cd821fa044d1eb49f03fb81a7acf3617", new BigInteger("30")));
        accounts.add(new Account("8313e2e711ce6d7246e8d5ce20ebcfa782d2d111", new BigInteger("50")));
        accounts.add(new Account("a809913b5a5193b477c51b4ba4aa0e1268ed6d13", new BigInteger("20")));
        accounts.add(new Account("da8123acd122960024c6e2ed52afa53078105127", new BigInteger("90")));
        accounts.add(new Account("33d2f8d22755e65fb0d92883f02413495ec3d9df", new BigInteger("40")));
        accounts.add(new Account("d9c93b27c02819a524e8b0e812e128367102f1a1", new BigInteger("70")));
        accounts.add(new Account("3fa509b6bd3d722fbd077d28f64f93cfab2e3958", new BigInteger("80")));
        accounts.add(new Account("f58d6a22b5ae704f4e37d6dd2dc07602b11c1800", new BigInteger("100")));

        MercleTree<Object, Object> mercleTree = new MercleTree(1l, new MercleStore(storePath, null), false);
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
            byte[] rootHash = mercleTree.commit(null);
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRoot().getHash()));
        }
    }

    /**
     * 각 버젼을 저장 할때의 Root Hash 값과 각 버젼마다 로드 후 Root Hash 값이 같은지를 비교한다. (각 버젼트리 로드후에는 전체 노드를 후위순회하여 Root Hash 계산)
     */
    @Test
    public void compareRootHashOfEachVersion() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        accounts.add(new Account("51e5ae98cd821fa044d1eb49f03fb81a7acf3617", new BigInteger("30")));
        accounts.add(new Account("8313e2e711ce6d7246e8d5ce20ebcfa782d2d111", new BigInteger("50")));
        accounts.add(new Account("a809913b5a5193b477c51b4ba4aa0e1268ed6d13", new BigInteger("20")));
        accounts.add(new Account("da8123acd122960024c6e2ed52afa53078105127", new BigInteger("90")));
        accounts.add(new Account("33d2f8d22755e65fb0d92883f02413495ec3d9df", new BigInteger("40")));
        accounts.add(new Account("d9c93b27c02819a524e8b0e812e128367102f1a1", new BigInteger("70")));
        accounts.add(new Account("3fa509b6bd3d722fbd077d28f64f93cfab2e3958", new BigInteger("80")));
        accounts.add(new Account("f58d6a22b5ae704f4e37d6dd2dc07602b11c1800", new BigInteger("100")));

        MercleTree<Object, Object> mercleTree = new MercleTree(1l, new MercleStore(storePath, null), false);
        Map<Long, byte[]> rootHashMap = new HashMap<>();
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
            byte[] rootHash = mercleTree.commit(null);
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRoot().getHash()));
            rootHashMap.put(mercleTree.getCurrentVersion(), rootHash);
        }

        for (long i = 1; i <= accounts.size(); i++) {
            mercleTree.loadVersion(i, mercleTree.isOffVersioning());
            assertEquals(Base64Util.encodeBase64(rootHashMap.get(i)), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
        }
    }

    /**
     * 각 버젼별 트리의 저장 구조와 각 버젼별 로드 후 저장 구조가 같은지를 트리 형태로 나타낸다.
     */
    @Test
    public void displayTreeStructureInEachVersion() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        accounts.add(new Account("51e5ae98cd821fa044d1eb49f03fb81a7acf3617", new BigInteger("30")));
        accounts.add(new Account("8313e2e711ce6d7246e8d5ce20ebcfa782d2d111", new BigInteger("50")));
        accounts.add(new Account("a809913b5a5193b477c51b4ba4aa0e1268ed6d13", new BigInteger("20")));
        accounts.add(new Account("da8123acd122960024c6e2ed52afa53078105127", new BigInteger("90")));
        accounts.add(new Account("33d2f8d22755e65fb0d92883f02413495ec3d9df", new BigInteger("40")));
        accounts.add(new Account("d9c93b27c02819a524e8b0e812e128367102f1a1", new BigInteger("70")));
        accounts.add(new Account("3fa509b6bd3d722fbd077d28f64f93cfab2e3958", new BigInteger("80")));
        accounts.add(new Account("f58d6a22b5ae704f4e37d6dd2dc07602b11c1800", new BigInteger("100")));

        MercleTree<Object, Object> mercleTree = new MercleTree(1l, new MercleStore(storePath, null), false);
        Map<Long, byte[]> rootHashMap = new HashMap<>();
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
            System.out.println(String.format("================ version : %s ================", mercleTree.getCurrentVersion() + 1));
            BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);
            byte[] rootHash = mercleTree.commit(null);
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRoot().getHash()));
            rootHashMap.put(mercleTree.getCurrentVersion(), rootHash);
        }

        for (long i = 1; i <= 10; i++) {
            mercleTree.loadVersion(i, mercleTree.isOffVersioning());
            assertEquals(Base64Util.encodeBase64(rootHashMap.get(i)), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
            System.out.println(String.format("================ version(reload) : %s ================", mercleTree.getCurrentVersion()));
            BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);
        }
    }

    /**
     * 이미 존재하는 Key의 값을 변경한 후 각 버젼별로 로드하여 시점 데이터를 정상적으로 조회되는지 검사한다.
     * 각 데이터 변경시 새롭게 저장되는 노드 데이터가 어떤것인지 트리 형태로 나타낸다.
     */
    @Test
    public void alterExistingDataOfNode() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        accounts.add(new Account("51e5ae98cd821fa044d1eb49f03fb81a7acf3617", new BigInteger("30")));
        accounts.add(new Account("8313e2e711ce6d7246e8d5ce20ebcfa782d2d111", new BigInteger("50")));
        accounts.add(new Account("a809913b5a5193b477c51b4ba4aa0e1268ed6d13", new BigInteger("20")));
        accounts.add(new Account("da8123acd122960024c6e2ed52afa53078105127", new BigInteger("90")));
        accounts.add(new Account("33d2f8d22755e65fb0d92883f02413495ec3d9df", new BigInteger("40")));
        accounts.add(new Account("d9c93b27c02819a524e8b0e812e128367102f1a1", new BigInteger("70")));
        accounts.add(new Account("3fa509b6bd3d722fbd077d28f64f93cfab2e3958", new BigInteger("80")));
        accounts.add(new Account("f58d6a22b5ae704f4e37d6dd2dc07602b11c1800", new BigInteger("100")));

        MercleTree<Object, Object> mercleTree = new MercleTree(null, new MercleStore(storePath, null), false);
        Map<Long, byte[]> rootHashMap = new HashMap<>();
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
            byte[] rootHash = mercleTree.commit(null);
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRoot().getHash()));
            rootHashMap.put(mercleTree.getCurrentVersion(), rootHash);
        }

        //Alter data (1)
        String alterKey = "f58d6a22b5ae704f4e37d6dd2dc07602b11c1800";
        Account account = mercleTree.get(alterKey);
        account.setBalance(new BigInteger("200"));
        mercleTree.put(alterKey, account);
        BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);
        byte[] rootHash = mercleTree.commit(null);
        assertEquals(rootHash, mercleTree.getRootHashThroughTraversalOfAllNodes());
        assertNotEquals(Base64Util.encodeBase64(rootHashMap.get((long) accounts.size())), Base64Util.encodeBase64(rootHash));

        Account alteredAccount = mercleTree.get(alterKey);
        assertEquals(new BigInteger("200").toString(), alteredAccount.getBalance().toString());

        mercleTree.loadVersion(mercleTree.getCurrentVersion() - 1, mercleTree.isOffVersioning());
        Account prevAccount = mercleTree.get(alterKey);
        assertEquals(new BigInteger("100").toString(), prevAccount.getBalance().toString());

        //Alter data (2)
        alterKey = "db0c9f45be6b121aaeef9e382320e0b156487b57";
        account = mercleTree.get(alterKey);
        account.setBalance(new BigInteger("20"));
        mercleTree.put(alterKey, account);
        BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);
        rootHash = mercleTree.commit(null);
        assertEquals(rootHash, mercleTree.getRootHashThroughTraversalOfAllNodes());
        assertNotEquals(Base64Util.encodeBase64(rootHashMap.get((long) accounts.size())), Base64Util.encodeBase64(rootHash));

        alteredAccount = mercleTree.get(alterKey);
        assertEquals(new BigInteger("20").toString(), alteredAccount.getBalance().toString());

        mercleTree.loadVersion(mercleTree.getCurrentVersion() - 2, mercleTree.isOffVersioning());
        prevAccount = mercleTree.get(alterKey);
        assertEquals(new BigInteger("10").toString(), prevAccount.getBalance().toString());
    }

    /**
     * 여러가지 데이터 타입을 Mercle Tree에 저장이 가능한지 검사한다.
     */
    @Test
    public void saveMultipleDataType() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        accounts.add(new Account("51e5ae98cd821fa044d1eb49f03fb81a7acf3617", new BigInteger("30")));
        accounts.add(new Account("8313e2e711ce6d7246e8d5ce20ebcfa782d2d111", new BigInteger("50")));
        accounts.add(new Account("a809913b5a5193b477c51b4ba4aa0e1268ed6d13", new BigInteger("20")));
        accounts.add(new Account("da8123acd122960024c6e2ed52afa53078105127", new BigInteger("90")));
        accounts.add(new Account("33d2f8d22755e65fb0d92883f02413495ec3d9df", new BigInteger("40")));
        accounts.add(new Account("d9c93b27c02819a524e8b0e812e128367102f1a1", new BigInteger("70")));
        accounts.add(new Account("3fa509b6bd3d722fbd077d28f64f93cfab2e3958", new BigInteger("80")));
        accounts.add(new Account("f58d6a22b5ae704f4e37d6dd2dc07602b11c1800", new BigInteger("100")));

        MercleTree<Object, Object> mercleTree = new MercleTree(null, new MercleStore(storePath, null), false);
        Map<Long, byte[]> rootHashMap = new HashMap<>();
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
            byte[] rootHash = mercleTree.commit(null);
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
            assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRoot().getHash()));
            rootHashMap.put(mercleTree.getCurrentVersion(), rootHash);
        }
        BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);

        Validator validator = new Validator("0x10", true);
        mercleTree.put(validator.getAddr(), validator);
        BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);
        byte[] rootHash = mercleTree.commit(null);
        assertEquals(rootHash, mercleTree.getRootHashThroughTraversalOfAllNodes());

        //새로운 Tree 버젼을 저장한 후 정상적으로 조회되는지 검사한다.
        Validator getValidator = mercleTree.get(validator.getAddr());
        assertNotNull(getValidator);
        assertEquals(validator.getAddr(), getValidator.getAddr());
        assertEquals(validator.isFreezing(), getValidator.isFreezing());

        //Validator 저장전 Tree 버젼을 로드하여 값이 없는지를 검사한다.
        mercleTree.loadVersion(10l, mercleTree.isOffVersioning());
        getValidator = mercleTree.get(validator.getAddr());
        assertNull(getValidator);

        //List 데이터도 정상적으로 저장되는지 검사한다.
        List<Validator> validators = new ArrayList<>();
        validators.add(new Validator("0x20", true));
        validators.add(new Validator("0x30", false));
        mercleTree.loadVersion(null, mercleTree.isOffVersioning());
        mercleTree.put("valList", validators);
        BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);
        rootHash = mercleTree.commit(null);
        assertEquals(rootHash, mercleTree.getRootHashThroughTraversalOfAllNodes());

        List<Validator> getValidators = mercleTree.get("valList");
        assertNotNull(getValidators);
        assertEquals("0x20", getValidators.get(0).getAddr());
        assertEquals(true, getValidators.get(0).isFreezing());
        assertEquals("0x30", getValidators.get(1).getAddr());
        assertEquals(false, getValidators.get(1).isFreezing());

        //Primitive Type
        mercleTree.loadVersion(null, mercleTree.isOffVersioning());
        mercleTree.put("1", 1);
        mercleTree.put("2", 2);
        mercleTree.put("true", new char[]{'c'});
        BTreePrinterUtil.printNode(mercleTree.getRoot(), 4);
        rootHash = mercleTree.commit(null);
        assertEquals(rootHash, mercleTree.getRootHashThroughTraversalOfAllNodes());
    }

    /**
     * Transaction의 Mercle Root를 구하는 테스트
     */
    @Test
    public void getRootHashOfTransactions() throws Exception {
        TreeMap<String, Transaction> transactions = new TreeMap<>();
        for (int i = 0; i < 10; i++) {
            JsonObject jsonArgument = new JsonObject();
            jsonArgument.addProperty(String.valueOf(i), String.valueOf(i));
            JsonArray body = new JsonArray();
            body.add(jsonArgument);
            Transaction tx = new Transaction(new TransactionHeader(BigInteger.valueOf(i).toByteArray(), BigInteger.valueOf(i).toByteArray(), BigInteger.valueOf(i).toByteArray(), new Date().getTime(), BigInteger.valueOf(i).toByteArray(), BigInteger.valueOf(i).toString().length()), BigInteger.valueOf(i).toByteArray(), new TransactionBody(body));
            transactions.put(Base64Util.encodeBase64(tx.toBinary()), tx);
        }
        MercleTree<String, Transaction> mercleTree = new MercleTree(null, null, false);
        mercleTree.put(transactions);
        BTreePrinterUtil.printNode(mercleTree.getRoot(), 3);
        //Commit 이용한 Root Hash 구함.
        byte[] rootHash = mercleTree.commit(null);

        mercleTree = new MercleTree(null, null, false);
        mercleTree.put(transactions);
        //후위순회를 통하여 Root Hash를 구함
        assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));
    }

    @Test
    public void testJsonObject() {
        JsonObject value = new JsonObject();
        value.add("a", new Gson().toJsonTree(1));
        MercleTree<Object, Object> mercleTree = new MercleTree(1l, new MercleStore(storePath, null), false);
        mercleTree.put("a", value.toString());
        mercleTree.commit(null);

        String data = mercleTree.get("a");
        JsonObject result = new JsonParser().parse(data).getAsJsonObject();
        assertEquals(value, result);
    }

    @Test
    public void displayTree() {
        boolean[] list = new boolean[]{true, false};
        MercleTree<Object, Object> mercleTree = new MercleTree(1l, new MercleStore(storePath, null), false);
        for (int i = 0; i < list.length; i++) {
            mercleTree.put(list[i], list[i]);
        }
        mercleTree.getRootHashThroughTraversalOfAllNodes();
    }

    /**
     * 버젼관리를 하지 않는 경우에 대해서 테스트를 진행한다.
     */
    @Test
    public void offVersioning() {
        MercleTree<Object, Object> mercleTree = new MercleTree(1l, new MercleStore(storePath, null), true);

        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
        }
        byte[] rootHash = mercleTree.commit(null);
        assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));

        mercleTree.put(accounts.get(0).getAddr(), new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("70")));
        rootHash = mercleTree.commit(null);
        assertEquals(Base64Util.encodeBase64(rootHash), Base64Util.encodeBase64(mercleTree.getRootHashThroughTraversalOfAllNodes()));

        long loadedVersion = mercleTree.loadVersion(1l, mercleTree.isOffVersioning());
        Account getAccount = mercleTree.get("f9041e835f17237f01257f7a0e978cdcee6403f6");
        assertEquals(2, loadedVersion);
        assertEquals("70", getAccount.getBalance().toString());
    }

    /**
     * 버젼관리를 Off 한 후 버젼관리 On 했을경우, 최초에 Off로 했기 때문에 그 설정이 적용되는지 테스트.
     */
    @Test
    public void impossibleSwitchOffVersioningToVersioning() {
        Store levelDBStore = new MercleStore(storePath, null);
        //off versioning
        MercleTree<Object, Object> mercleTree = new MercleTree(1l, levelDBStore, true);

        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
            mercleTree.commit(null);
        }

        mercleTree = new MercleTree(1l, levelDBStore, false);
        long version = mercleTree.loadVersion(1l, mercleTree.isOffVersioning());
        assertEquals(2, version);
        assertEquals(true, mercleTree.isOffVersioning());

        assertNotNull(mercleTree.get("db0c9f45be6b121aaeef9e382320e0b156487b57"));
    }

    /**
     * 버젼관리를 On 한 후 버젼관리 Off 했을경우, 최초에 On로 했기 때문에 그 설정이 적용되는지 테스트.
     */
    @Test
    public void impossibleSwitchVersioningToOffVersioning() {
        Store levelDBStore = new MercleStore(storePath, null);
        //versioning
        MercleTree<Object, Object> mercleTree = new MercleTree(1l, levelDBStore, false);

        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("f9041e835f17237f01257f7a0e978cdcee6403f6", new BigInteger("60")));
        accounts.add(new Account("db0c9f45be6b121aaeef9e382320e0b156487b57", new BigInteger("10")));
        for (Account account : accounts) {
            mercleTree.put(account.getAddr(), account);
            mercleTree.commit(null);
        }

        mercleTree = new MercleTree(2l, levelDBStore, true);
        long version = mercleTree.loadVersion(1l, mercleTree.isOffVersioning());
        assertEquals(1, version);
        assertEquals(false, mercleTree.isOffVersioning());

        assertNull(mercleTree.get("db0c9f45be6b121aaeef9e382320e0b156487b57"));
    }
}
