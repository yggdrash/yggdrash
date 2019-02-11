package io.yggdrash.common.util;


import io.yggdrash.core.store.MercleTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BTreePrinterUtil {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static <T extends Comparable<?>> void printNode(MercleTree.Node root, int maxKeySize) {
        int maxLevel = BTreePrinterUtil.maxLevel(root);

        printNodeInternal(Collections.singletonList(root), 1, maxLevel + 2, maxKeySize);
    }

    private static <T extends Comparable<?>> void printNodeInternal(List<MercleTree.Node> nodes, int level, int maxLevel, int maxKeySize) {
        if (nodes.isEmpty() || BTreePrinterUtil.isAllElementsNull(nodes))
            return;

        int floor = maxLevel - level;
        int endgeLines = (int) Math.pow(2, (Math.max(floor - 1, 0)));
        int firstSpaces = (int) Math.pow(2, (floor)) - 1;
        int betweenSpaces = (int) Math.pow(2, (floor + 1)) - 1;


        BTreePrinterUtil.printWhitespaces(firstSpaces);

        List<MercleTree.Node> newNodes = new ArrayList<>();
        int subSpace = maxKeySize;
        for (MercleTree.Node node : nodes) {
            if (node != null) {
                String nodeKey = node.getKey().toString();
                int keySize = nodeKey.length();
                for (int i = 0; i < maxKeySize - keySize; i++) {
                    nodeKey = "0" + nodeKey;
                }
                if (nodeKey.length() > maxKeySize) {
                    nodeKey = nodeKey.substring(0, maxKeySize);
                }
                if (node.isUpdate()) {
                    AnsiColorUtil.displayMessage(AnsiColorUtil.Color.RED, nodeKey);
                } else {
                    System.out.print(nodeKey);
                }
                newNodes.add(node.getLeft());
                newNodes.add(node.getRight());
            } else {
                newNodes.add(null);
                newNodes.add(null);
                System.out.print(" ");
            }

            int space = betweenSpaces;
            if (subSpace > 0) {
                space -= (subSpace--);
            }
            BTreePrinterUtil.printWhitespaces(space);
        }
        System.out.println("");

        for (int i = 1; i <= endgeLines; i++) {
            for (int j = 0; j < nodes.size(); j++) {
                BTreePrinterUtil.printWhitespaces(firstSpaces - i);
                if (nodes.get(j) == null) {
                    BTreePrinterUtil.printWhitespaces(endgeLines + endgeLines + i + 1);
                    continue;
                }

                if (nodes.get(j).getLeft() != null)
                    System.out.print("/");
                else
                    BTreePrinterUtil.printWhitespaces(1);

                BTreePrinterUtil.printWhitespaces(i + i - 1);

                if (nodes.get(j).getRight() != null)
                    System.out.print("\\");
                else
                    BTreePrinterUtil.printWhitespaces(1);

                BTreePrinterUtil.printWhitespaces(endgeLines + endgeLines - i);
            }

            System.out.println("");
        }

        printNodeInternal(newNodes, level + 1, maxLevel, maxKeySize);
    }

    private static void printWhitespaces(int count) {
        for (int i = 0; i < count; i++)
            System.out.print(" ");
    }

    private static <T extends Comparable<?>> int maxLevel(MercleTree.Node node) {
        if (node == null)
            return 0;

        return Math.max(BTreePrinterUtil.maxLevel(node.getLeft()), BTreePrinterUtil.maxLevel(node.getRight())) + 1;
    }

    private static <T> boolean isAllElementsNull(List<T> list) {
        for (Object object : list) {
            if (object != null)
                return false;
        }

        return true;
    }
}
