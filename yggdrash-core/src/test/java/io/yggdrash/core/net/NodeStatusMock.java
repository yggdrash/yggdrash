package io.yggdrash.core.net;

public class NodeStatusMock implements NodeStatus {
    public static final NodeStatus mock = new NodeStatusMock();

    private String status = "up";

    @Override
    public boolean isUpStatus() {
        return status.equals("up");
    }

    @Override
    public void up() {
        status = "up";
    }

    @Override
    public void sync() {
        status = "sync";
    }
}
