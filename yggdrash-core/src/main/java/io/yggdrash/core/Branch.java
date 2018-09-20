package io.yggdrash.core;

import java.util.List;

public class Branch {
    public static final String STEM = "STEM";
    public static final String YEED = "YEED";

    private BranchId branchId;

    private String name;
    private String owner;
    private String symbol;
    private String property;
    private String type;
    private String timestamp;
    private float tag;
    private String version;
    private List<String> versionHistory;
    private String referenceAddress;
    private String reserveAddress;

    public BranchId getBranchId() {
        return branchId;
    }

    public void setBranchId(BranchId branchId) {
        this.branchId = branchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isYeed() {
        return YEED.equals(name);
    }

    public static Branch of(String branchId, String name, String owner) {
        Branch branch = new Branch();
        branch.branchId = BranchId.of(branchId);
        branch.name = name;
        branch.owner = owner;
        return branch;
    }
}
