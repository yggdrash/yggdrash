package io.yggdrash.core.exception;

public class NonExistObjectException extends NullPointerException {

    public static final int CODE = -10000;

    public NonExistObjectException() {
        super();
    }

    public NonExistObjectException(String msg) {
        super(msg + " not found");
    }

    public static class BranchNotFound extends NonExistObjectException {

        public BranchNotFound() {
            super("Branch");
        }

        public BranchNotFound(String branchId) {
            super("Branch " + branchId);
        }

    }
    public static class BlockNotFound extends NonExistObjectException {

        public BlockNotFound() {
            super("Block");
        }

        public BlockNotFound(String blockId) {
            super("Block " + blockId);
        }

    }

    public static class TxNotFound extends NonExistObjectException {
        public TxNotFound() {
            super("Transaction");
        }

        public TxNotFound(String txId) {
            super("Transaction " + txId);
        }
    }

    public static class ValidatorsNotFound extends NonExistObjectException {
        public ValidatorsNotFound() {
            super("Validators");
        }
    }

    public static class TagNotFound extends NonExistObjectException {
        public TagNotFound(String tag) {
            super("Tag '" + tag + "'");
        }
    }
}