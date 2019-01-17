package io.yggdrash.core.net;

import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class KademliaDiscovery implements Discovery {
    private static final Logger log = LoggerFactory.getLogger(KademliaDiscovery.class);

    private PeerGroup peerGroup;
    private Peer owner;

    @Override
    public void setPeerGroup(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
        this.owner = peerGroup.getOwner();
    }

    @Override
    public PeerGroup getPeerGroup() {
        return peerGroup;
    }

    @Override
    public void discover() {
        for (String ynodeUri : peerGroup.getBootstrappingSeedList()) {
            String ynodeUriWithoutPubKey = peerGroup.getOwner().getYnodeUri()
                    .substring(ynodeUri.indexOf("@"));
            if (ynodeUri.contains(ynodeUriWithoutPubKey)) {
                continue;
            }
            Peer peer = Peer.valueOf(ynodeUri);
            PeerClientChannel client = getClient(peer);
            log.info("Try connecting to SEED peer = {}", peer);

            try {
                List<Proto.PeerInfo> foundedPeerList = client.findPeers(peerGroup.getOwner());
                for (Proto.PeerInfo peerInfo : foundedPeerList) {
                    peerGroup.addPeerByYnodeUri(peerInfo.getUrl());
                }
            } catch (Exception e) {
                log.error("Failed connecting to SEED peer = {}", peer);
            } finally {
                client.stop();
            }
        }

        int peerCount = peerGroup.count();
        log.info("Start discover! peerCount={}", peerCount);
        findPeers(0, new ArrayList<>());
    }

    protected abstract PeerClientChannel getClient(Peer peer);

    private synchronized void findPeers(int round, List<Peer> prevTried) {
        try {
            if (round == KademliaOptions.MAX_STEPS) {
                log.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover"
                        + "after %d rounds.", round));
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerGroup.count()),
                        peerGroup.getPeerUriList());
                return;
            }

            List<Peer> closest = peerGroup.getClosestPeers();
            List<Peer> tried = new ArrayList<>();

            for (Peer peer : closest) {
                if (!tried.contains(peer) && !prevTried.contains(peer)) {
                    PeerClientChannel clientChannel = getClient(peer);
                    try {
                        Optional<List<Proto.PeerInfo>> list = Optional.ofNullable(
                                clientChannel.findPeers(owner));
                        list.ifPresent(nodeInfo -> nodeInfo.forEach(
                                p -> peerGroup.addPeerByYnodeUri(p.getUrl())));

                        tried.add(peer);
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    } finally {
                        clientChannel.stop();
                    }
                }
                if (tried.size() == KademliaOptions.ALPHA) {
                    break;
                }
            }

            if (tried.isEmpty()) {
                log.debug("Terminating discover after {} rounds.", round);
                log.trace("{}\n{}",
                        String.format("Peers discovered %d", peerGroup.count()),
                        peerGroup.getPeerUriList());
                return;
            }

            tried.addAll(prevTried);
            findPeers(round + 1, tried);
        } catch (Exception e) {
            log.info("{}", e);
        }
    }
}
