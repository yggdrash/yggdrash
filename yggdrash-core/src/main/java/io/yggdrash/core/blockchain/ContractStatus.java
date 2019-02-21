package io.yggdrash.core.blockchain;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ContractStatus {
    private String version;
    private long id;
    private String location;
    private Status status;

    public ContractStatus(String version, long id, String location, int state) {
        this.version = version;
        this.id = id;
        this.location = location;
        this.status = Status.fromValue(state);
    }

    public String getVersion() {
        return version;
    }

    public long getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        UNKNOWN(0),
        UNINSTALLED(1),
        INSTALLED(2),
        RESOLVED(4),
        STARTING(8),
        STOPPING(10),
        ACTIVE(32);

        private int value;

        Status(int value) {
            this.value = value;
        }

        @JsonCreator
        public static Status fromValue(int value) {
            switch (value) {
                case 0:
                    return UNKNOWN;
                case 1:
                    return UNINSTALLED;
                case 2:
                    return INSTALLED;
                case 4:
                    return RESOLVED;
                case 8:
                    return STARTING;
                case 10:
                    return STOPPING;
                case 32:
                    return ACTIVE;
                default:
                    return null;
            }
        }
    }
}
