package io.yggdrash.core.exception;

public class WrongStructuredException extends IllegalArgumentException {
    public static final int CODE = -10002;
    public static final String MSG = "Wrong structure exception";

    WrongStructuredException() {
        super(MSG);
    }

    WrongStructuredException(String msg) {
        super(msg);
    }

    public static class InvalidBranch extends WrongStructuredException {
        public InvalidBranch() {
            super("Invalid branch format");
        }
    }

    public static class InvalidTx extends WrongStructuredException {
        public InvalidTx() {
            super("Invalid transaction format");
        }
    }

    public static class InvalidRawTx extends WrongStructuredException {
        public InvalidRawTx() {
            super("Invalid raw transaction format");
        }
    }

    public static class InvalidBlock extends WrongStructuredException {
        public InvalidBlock() {
            super("Invalid block format");
        }
    }

}
