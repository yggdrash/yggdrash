package io.yggdrash.core.blockchain.osgi;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ContractStatus {
    private final String symbolicName;
    private final String version;
    private final String vendor;
    private final String description;
    private final long id;
    private final String location;
    private final Status status;
    private final int serviceCnt;

    public ContractStatus(String symbolicName, String version, String vendor, String description,
                   long id, String location, int state, int serviceCnt) {
        this.symbolicName = symbolicName;
        this.version = version;
        this.vendor = vendor == null ? "" : vendor;
        this.description = description == null ? "" : description;
        this.id = id;
        this.location = location;
        this.status = Status.fromValue(state);
        this.serviceCnt = serviceCnt;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getVersion() {
        return version;
    }

    public String getVendor() {
        return vendor;
    }

    public String getDescription() {
        return description;
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

    public int getServiceCnt() {
        return serviceCnt;
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
