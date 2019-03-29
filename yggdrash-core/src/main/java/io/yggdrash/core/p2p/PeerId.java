package io.yggdrash.core.p2p;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.crypto.HashUtil;

public class PeerId {
    private final Sha3Hash id;

    private PeerId(Sha3Hash hash) {
        this.id = hash;
    }

    private PeerId(byte[] bytes) {
        this(Sha3Hash.createByHashed(bytes));
    }

    public byte[] getBytes() {
        return this.id.getBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PeerId peerId = (PeerId) o;
        return id.equals(peerId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public static PeerId of(byte[] hash) {
        return new PeerId(hash);
    }

    public static PeerId of(String ynodeUri) {
        return of(HashUtil.sha3omit12(ynodeUri.getBytes()));
    }

    int distanceTo(byte[] targetId) {
        byte[] ownerId = id.getBytes();
        byte[] hash = new byte[Math.min(ownerId.length, targetId.length)];

        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) (((int) ownerId[i]) ^ ((int) targetId[i]));
        }

        int distance = KademliaOptions.BINS;

        for (byte b : hash) {
            if (b == 0) {
                distance -= 8;
            } else {
                int cnt = 0;
                for (int i = 7; i >= 0; i--) {
                    boolean a = ((b & 0xff) & (1 << i)) == 0;
                    if (a) {
                        cnt++;
                    } else {
                        break;
                    }
                }
                distance -= cnt;
                break;
            }
        }
        return distance;
    }
}
