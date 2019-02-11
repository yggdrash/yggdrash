package io.yggdrash.core.store;

import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.SerializeUtil;
import io.yggdrash.core.exception.SerializeException;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class MercleTree<K, V> {
    //Save last version key (auto increment)
    private static final String LAST_VERSION_KEY = "lvk";
    //Save the root node in each version (When doing version control)
    private static final String EACH_VERSION_KEY = "evk-%s";
    //When not versioning
    private static final String LAST_ROOT_NODE_KEY = "lrn";
    //Whether version is managed at first load
    private static final String OFF_VERSIONING_INFO = "ovi";

    private Store<Object, Object> store;

    private Node<K, V> root;
    //List of nodes to save
    private Map<K, Node<K, V>> updatedNodes;
    private boolean offVersioning;
    private long currentVersion;

    public MercleTree() {
        this(null, null, true);
    }

    public MercleTree(Long currentVersion, Store<Object, Object> store, boolean offVersioning) {
        this.store = store;
        this.updatedNodes = new HashMap<>();
        this.offVersioning = loadVersioning(offVersioning);
        this.currentVersion = loadVersion(currentVersion, this.offVersioning);
    }

    public long getCurrentVersion() {
        return currentVersion;
    }

    public Node<K, V> getRoot() {
        return root;
    }

    public boolean isOffVersioning() {
        return offVersioning;
    }

    private boolean loadVersioning(boolean offVersioning) {
        // Whether versioning or off versioning is stored.
        Boolean storedOffVersioning = getFromStore((K) OFF_VERSIONING_INFO);
        if (storedOffVersioning == null) {
            storedOffVersioning = offVersioning;
            putToStore((K) OFF_VERSIONING_INFO, offVersioning);
        }

        return storedOffVersioning;
    }

    public long loadVersion(Long version, boolean offVersioning) {
        long loadedVersion = 0;
        if (offVersioning || version == null) {
            TreeVersion lastTreeVersion = getLastTreeVersion();
            if (lastTreeVersion != null) {
                loadedVersion = lastTreeVersion.getVersion();
            }
        } else {
            loadedVersion = version;
        }


        if (offVersioning) {
            root = getFromStore((K) LAST_ROOT_NODE_KEY);
        } else {
            root = getFromStore((K) String.format(EACH_VERSION_KEY, loadedVersion));
        }

        if (root != null) {
            root.setStore(store);
            root.setOffVersioning(offVersioning);
        }

        return loadedVersion;
    }

    public synchronized V put(TreeMap<K, V> values) {
        final Object[] rootNode = new Object[1];
        values.forEach((k, v) -> rootNode[0] = put(k, v));
        return (V) rootNode[0];
    }

    /**
     * Associates the specified value with the specified key in this tree
     * If the tree previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return previously contained value for the key
     */
    public synchronized V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("Key is null");
        }

        //1. insert root node
        Node<K, V> tempParent = root;
        if (tempParent == null) {
            tempParent = get(key);
            if (tempParent == null) {
                root = new Node(key, value, this.offVersioning, store);
                root.setRoot(true);
                root.setHash(updatedNodes);
                return null;
            }
            root = tempParent;
        }

        Object[] prevValue = new Object[1];

        insert(tempParent, key, value, prevValue);
        return (V) prevValue[0];
    }

    public <V> V get(K key) {
        if (root == null) {
            return null;
        }
        return findKey(key, root);
    }

    public synchronized byte[] commit(Long version) {
        TreeVersion treeVersion = new TreeVersion(version == null ? 0 : version);
        if (version == null) {
            treeVersion = getLastTreeVersion();
            if (treeVersion == null) {
                treeVersion = new TreeVersion();
            }
            treeVersion.increment();
        }

        // Save updated node
        Iterator<K> iter = updatedNodes.keySet().iterator();
        while (iter.hasNext()) {
            Node node = updatedNodes.get(iter.next());
            node.setVersion(treeVersion.getVersion());
            putToStore((K) (offVersioning ? node.getKey() : node.getHash()), node);
        }

        putToStore((K) LAST_VERSION_KEY, treeVersion);
        if (offVersioning) {
            putToStore((K) LAST_ROOT_NODE_KEY, root);
        } else {
            putToStore((K) String.format(EACH_VERSION_KEY, treeVersion.getVersion()), root);
        }

        this.currentVersion = treeVersion.getVersion();
        root.left = null;
        root.right = null;
        updatedNodes = new HashMap<>();
        return root.getHash();
    }

    //For test
    public synchronized byte[] getRootHashThroughTraversalOfAllNodes() {
        Node root = null;
        List<Node> orderList = postOrderTraversal();
        for (Node node : orderList) {
            node.exchangeTempChildToChild();
            node.setHash(null);
            if (node.isRoot()) {
                root = node;
            }
        }

        byte[] rootHash = root.getHash();
        this.root = root;
        return rootHash;
    }

    private List<Node> postOrderTraversal() {
        List<Node> orderList = new ArrayList();

        if (root == null) {
            return orderList;
        }

        Stack<Node> stack = new Stack();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node item = stack.peek();
            if (item.getLeft() == null && item.getRight() == null) {
                orderList.add(stack.pop());
            } else {
                if (item.getRight() != null) {
                    stack.push(item.getRight());
                    item.setTempRight(item.getRight());
                    item.setRight(null, updatedNodes);
                }

                if (item.getLeft() != null) {
                    stack.push(item.getLeft());
                    item.setTempLeft(item.getLeft());
                    item.setLeft(null, updatedNodes);
                }
            }
        }

        return orderList;
    }

    private int height(Node node) {
        if (node == null) {
            return 0;
        }
        return node.height;
    }

    private int max(int leftH, int rightH) {
        return leftH > rightH ? leftH : rightH;
    }

    private TreeVersion getLastTreeVersion() {
        if (store == null) {
            return null;
        }
        return store.get(LAST_VERSION_KEY);
    }

    private <V> V findKey(K key, Node node) {
        if (node == null) {
            return null;
        }
        int cmp = ((Comparable) key).compareTo(node.getKey());

        if (cmp == 0) {
            return (V) node.getValue();
        } else if (cmp < 0) {   //Left
            return findKey(key, node.getLeft());
        } else {    //Right
            return findKey(key, node.getRight());
        }
    }

    private void updateNodeByRotate(Node originInNode, Node newInNode, Node otherNode) {
        // Update heights
        originInNode.setHeight(max(height(originInNode.getLeft()), height(originInNode.getRight())) + 1);
        newInNode.setHeight(max(height(newInNode.getLeft()), height(newInNode.getRight())) + 1);

        // Return new root
        if (originInNode.isRoot()) {
            originInNode.setRoot(false);
            newInNode.setRoot(true);
            root = newInNode;
        }

        // Update
        if (originInNode != null) {
            originInNode.setHash(updatedNodes);
        }
        if (newInNode != null) {
            newInNode.setHash(updatedNodes);
        }
        if (otherNode != null) {
            otherNode.setHash(updatedNodes);
        }
    }

    private Node rotateToRight(Node node) {
        Node inNode = node.getLeft();
        Node leftOfOriginNode = inNode.getRight();

        // Perform rotation
        node.setLeft(leftOfOriginNode, updatedNodes);
        inNode.setRight(node, updatedNodes);

        // Update node height with root
        updateNodeByRotate(node, inNode, leftOfOriginNode);
        return inNode;
    }

    private Node rotateToLeft(Node node) {
        Node inNode = node.getRight();
        Node rightOfOriginNode = inNode.getLeft();

        // Perform rotation
        node.setRight(rightOfOriginNode, updatedNodes);
        inNode.setLeft(node, updatedNodes);

        // Update node height with root
        updateNodeByRotate(node, inNode, rightOfOriginNode);
        return inNode;
    }

    private Node insert(Node node, K key, V value, Object[] prevValue) {
        if (node == null) {
            Node newNode = new Node(key, value, this.offVersioning, store);
            newNode.setHash(updatedNodes);
            return newNode;
        }

        int cmp = ((Comparable) key).compareTo(node.getKey());
        if (cmp == 0) {
            prevValue[0] = node.getValue();
            node.setValue(value);
            node.setHash(updatedNodes);
            return node;
        } else if (cmp < 0) {   //Left
            node.setLeft(insert(node.getLeft(), key, value, prevValue), updatedNodes);
        } else {    //Right
            node.setRight(insert(node.getRight(), key, value, prevValue), updatedNodes);
        }

        node.setHeight(max(height(node.getLeft()), height(node.getRight())) + 1);
        //balance factor (왼쪽 서브트리의 높이 - 오른쪽 서브트리 높이)
        //BF가 2이상 또는 -2이하인 경우 균형을 맞춘다.
        int bf = height(node.getLeft()) - height(node.getRight());

        if (bf > 1 && node.getLeft() != null) {
            cmp = ((Comparable) key).compareTo(node.getLeft().getKey());
            if (cmp < 0) {  //LL (rotate to right)
                return rotateToRight(node);
            } else if (cmp > 0) {   //LR (rotate to right > rotate to left)
                node.setLeft(rotateToLeft(node.getLeft()), updatedNodes);
                return rotateToRight(node);
            }
        }


        if (bf < -1 && node.getRight() != null) {
            cmp = ((Comparable) key).compareTo(node.getRight().getKey());
            if (cmp > 0) {
                return rotateToLeft(node);
            } else if (cmp < 0) {
                node.setRight(rotateToRight(node.getRight()), updatedNodes);
                return rotateToLeft(node);
            }
        }

        node.setHash(updatedNodes);
        return node;
    }

    private void putToStore(K key, Object value) {
        if (store == null) {
            return;
        }

        try {
            store.put(key, value);
        } catch (Exception e) {
            throw new SerializeException(e.getCause());
        }
    }

    private <V> V getFromStore(K key) {
        if (store == null) {
            return null;
        }

        try {
            return store.get(key);
        } catch (Exception e) {
            throw new SerializeException(e.getCause());
        }
    }

    public static final class TreeVersion<K> implements Serializable {
        private long version;

        TreeVersion() {

        }

        TreeVersion(long version) {
            this.version = version;
        }

        public long getVersion() {
            return version;
        }

        private void increment() {
            this.version++;
        }
    }

    //E - Element
    //K - rootKey
    //N - Number
    //T - Type
    //V - Value
    public static final class Node<K, V> implements Serializable {
        K key;
        V value;
        long version;
        int height;

        boolean isRoot;

        transient boolean offVersioning;
        transient boolean isUpdate;
        transient Node<K, V> left;
        transient Node<K, V> right;
        K leftKey;
        K rightKey;

        transient Node<K, V> tempLeft;
        transient Node<K, V> tempRight;

        transient Store<Object, Object> store;
        byte[] hash;

        Node(K key, V value, boolean offVersioning, Store<Object, Object> store) {
            this.key = key;
            this.value = value;
            this.offVersioning = offVersioning;
            this.store = store;
            this.isUpdate = true;
            this.height = 1;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public long getVersion() {
            return version;
        }

        public void setVersion(long version) {
            this.version = version;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public boolean isRoot() {
            return isRoot;
        }

        public void setRoot(boolean root) {
            isRoot = root;
        }

        public void setOffVersioning(boolean offVersioning) {
            this.offVersioning = offVersioning;
        }

        public boolean isUpdate() {
            return isUpdate;
        }

        private <V> V getFromStore(K key) {
            if (store == null) {
                return null;
            }

            try {
                Node node = store.get(key);
                if (node != null) {
                    if (this.offVersioning && key.equals(node.getKey())) {
                        return (V) node;
                    } else if (Arrays.equals((byte[]) key, node.getHash())) {
                        return (V) node;
                    }
                }
            } catch (Exception e) {
                throw new SerializeException(e.getCause());
            }

            return null;
        }

        public void setLeft(Node<K, V> left, Map<K, Node<K, V>> updatedNodes) {
            if (left == null) {
                this.left = null;
                this.leftKey = null;
                return;
            }
            left.setHash(updatedNodes);
            this.left = left;
            if (offVersioning) {
                this.leftKey = left.getKey();
            } else {
                this.leftKey = (K) left.getHash();
            }
        }

        public Node<K, V> getLeft() {
            if (leftKey != null && left == null) {
                left = getFromStore(leftKey);
                if (left != null) {
                    left.setStore(store);
                }
            }
            return left;
        }

        public void setRight(Node<K, V> right, Map<K, Node<K, V>> updatedNodes) {
            if (right == null) {
                this.right = null;
                this.rightKey = null;
                return;
            }
            right.setHash(updatedNodes);
            this.right = right;
            if (offVersioning) {
                this.rightKey = right.getKey();
            } else {
                this.rightKey = (K) right.getHash();
            }
        }

        public Node<K, V> getRight() {
            if (rightKey != null && right == null) {
                right = getFromStore(rightKey);
                if (right != null) {
                    right.setStore(store);
                }
            }
            return right;
        }

        public void exchangeTempChildToChild() {
            left = tempLeft;
            if (tempLeft != null) {
                leftKey = offVersioning ? tempLeft.key : (K) tempLeft.getHash();
            }
            tempLeft = null;

            right = tempRight;
            if (tempRight != null) {
                rightKey = offVersioning ? tempRight.key : (K) tempRight.getHash();
            }
            tempRight = null;
        }

        public void setTempLeft(Node<K, V> tempLeft) {
            this.tempLeft = tempLeft;
        }

        public void setTempRight(Node<K, V> tempRight) {
            this.tempRight = tempRight;
        }

        public void setStore(Store<Object, Object> store) {
            this.store = store;
        }

        public void setHash(Map<K, Node<K, V>> updatedNodes) {
            byte[] hash = SerializeUtil.serialize(value);
            if (getLeft() != null) {
                hash = ArrayUtils.addAll(hash, left.getHash());
            }
            if (getRight() != null) {
                hash = ArrayUtils.addAll(hash, right.getHash());
            }
            byte[] tempHash = HashUtil.sha3(hash);
            if (!Arrays.equals(this.hash, tempHash)) {
                this.hash = tempHash;
                this.isUpdate = true;
                if (updatedNodes != null && updatedNodes.get(key) == null) {
                    updatedNodes.put(key, this);
                }
            }
        }

        public byte[] getHash() {
            return hash;
        }
    }
}

