package io.yggdrash.core.exception;

import org.apache.commons.codec.DecoderException;

public class DecodeException extends RuntimeException {

    public static final int CODE = -10004;

    public DecodeException() {
        super();
    }

    public DecodeException(String str) {
        super("Decode " + str + " failed. " + str + " is not hex string.");
    }

    public static class TxIdNotHexString extends DecodeException {
        public TxIdNotHexString() {
            super("txId");
        }
    }

    public static class BranchIdNotHexString extends DecodeException {
        public BranchIdNotHexString() {
            super("branchId");
        }
    }

    public static class BlockIdNotHexString extends DecodeException {
        public BlockIdNotHexString() {
            super("blockId");
        }
    }

    public static class ContractVersionNotHexString extends DecodeException {
        public ContractVersionNotHexString() {
            super("contractVersion");
        }
    }

}