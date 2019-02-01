package io.yggdrash.validator.data.pbft;

public enum MessageType {
    REQUEST,
    PRE_PREPARE,
    PREPARE,
    COMMIT,
    REPLY,
    VIEW_CHANGE,
    NEW_VIEW,
    CHECKPOINT
}
