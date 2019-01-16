package io.yggdrash.core.net;

import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KademliaDiscovery implements Discovery {
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
            PeerHandler peerHandler = peerGroup.getPeerHandlerFactory().create(peer);
            log.info("Try connecting to SEED peer = {}", peer);

            try {
                Optional<List<String>> list = Optional.ofNullable(
                        peerHandler.findPeers(owner));
                list.ifPresent(nodeInfo -> nodeInfo.forEach(
                        uri -> peerGroup.addPeerByYnodeUri(uri)));
            } catch (Exception e) {
                log.error("Failed connecting to SEED peer = {}", peer);
            } finally {
                peerHandler.stop();
            }
        }

        int peerCount = peerGroup.count();
        log.info("Start discover! peerCount={}", peerCount);
        findPeers(0, new ArrayList<>());
    }

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
                    PeerHandler peerHandler = peerGroup.getPeerHandlerFactory().create(peer);
                    try {
                        Optional<List<String>> list = Optional.ofNullable(
                                peerHandler.findPeers(owner));
                        list.ifPresent(nodeInfo -> nodeInfo.forEach(
                                uri -> peerGroup.addPeerByYnodeUri(uri)));

                        tried.add(peer);
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    } finally {
                        peerHandler.stop();
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
