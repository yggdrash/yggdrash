package io.yggdrash.core.exception;

public class RejectedAccessException extends RuntimeException {
    public static final int CODE = -10003;
    public static final String MSG = "Rejected";

    public RejectedAccessException() {
        super(MSG);
    }

    public RejectedAccessException(String msg) {
        super(msg);
    }

    public static class NotFullSynced extends WrongStructuredException {
        public NotFullSynced() {
            super(MSG + ". Not yet full synced.");
        }
    }
}
