package ru.kuznetsov.qaip.simulation;

import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

class RelationshipIdentityPolicy {
    private static final String POLICY_VERSION = "qarel-id-v1";
    private static final String ID_PREFIX = "SIMREL-v1-";

    String idFor(RelationshipType type, String fromNodeId, String toNodeId) {
        String identity = String.join("\u0000",
                POLICY_VERSION, type.name(), fromNodeId, toNodeId);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    identity.getBytes(StandardCharsets.UTF_8));
            return ID_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new SimulationException(
                    SimulationErrorCode.INVALID_CANDIDATE_MODEL_SHAPE,
                    "Unable to generate a deterministic relationship ID"
            );
        }
    }
}
