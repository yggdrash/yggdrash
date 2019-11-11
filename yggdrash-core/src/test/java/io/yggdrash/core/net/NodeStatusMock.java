package io.yggdrash.core.net;

public class NodeStatusMock implements NodeStatus {
    public static final NodeStatus mock = new NodeStatusMock();

    private String status = "up";

    @Override
    public boolean isUpStatus() {
        return status.equals("up");
    }

    @Override
    public boolean isSyncStatus() {
        return status.equals("sync");
    }

    @Override
    public boolean isUpdateStatus() {
        return status.equals("update");
    }

    @Override
    public void up() {
        status = "up";
    }

    @Override
    public void sync() {
        status = "sync";
    }

    @Override
    public void update() {
        status = "update";
    }

    public static NodeStatus create() {
        return new NodeStatusMock();
    }
}
