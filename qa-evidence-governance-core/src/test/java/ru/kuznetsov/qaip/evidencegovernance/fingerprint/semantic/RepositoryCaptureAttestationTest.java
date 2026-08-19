package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintInput;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryCaptureAttestationTest {
    private static final String SOURCE = "repository:orders";
    private static final String SNAPSHOT = "capture:17";

    @Test
    void acceptsExactApprovedCaptureBinding() {
        var input = input(List.of(member("a.scenario.json", "one")), List.of(unsupported("z.scenario.json")));
        var fingerprint = RepositoryCaptureFingerprintEncoder.fingerprint(input);

        var attestation = attest(input, fingerprint, references(input, fingerprint),
                input.unsupportedMatchingEntries());

        assertEquals(input, attestation.fingerprintInput());
        assertEquals(fingerprint, attestation.contentFingerprint());
        assertEquals(input.capturedMembers().size(), attestation.regularMembers().size());
    }

    @Test
    void rejectsFingerprintAndForeignCaptureSubstitutionIncludingConsistentRelabeling() {
        var captureA = input(List.of(member("a.scenario.json", "capture-a")), List.of());
        var captureB = input(List.of(member("b.scenario.json", "capture-b")), List.of());
        var fingerprintA = RepositoryCaptureFingerprintEncoder.fingerprint(captureA);
        var fingerprintB = RepositoryCaptureFingerprintEncoder.fingerprint(captureB);

        assertThrows(IllegalArgumentException.class, () ->
                attest(captureA, fingerprintB, references(captureA, fingerprintB), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                attest(captureB, fingerprintB, references(captureA, fingerprintB), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                attest(captureA, fingerprintA, references(captureB, fingerprintA), List.of()));
    }

    @Test
    void rejectsChangedMissingExtraOrReorderedRegularMembers() {
        var input = input(List.of(member("a.scenario.json", "one"), member("b.scenario.json", "two")), List.of());
        var fingerprint = RepositoryCaptureFingerprintEncoder.fingerprint(input);
        var exact = references(input, fingerprint);
        var first = exact.getFirst();

        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint,
                List.of(changed(first, "changed.scenario.json", first.rawByteLength(),
                        first.rawMemberFingerprint())), List.of()));
        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint,
                List.of(changed(first, first.normalizedRepositoryRelativePath(),
                        first.rawByteLength().add(java.math.BigInteger.ONE), first.rawMemberFingerprint()),
                        exact.get(1)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint,
                List.of(changed(first, first.normalizedRepositoryRelativePath(), first.rawByteLength(),
                        RawSourceMemberFingerprint.calculate(new byte[]{99})), exact.get(1)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint,
                List.of(first), List.of()));
        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint,
                List.of(first, exact.get(1), exact.get(1)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint,
                List.of(exact.get(1), first), List.of()));
    }

    @Test
    void rejectsMissingExtraOrReorderedUnsupportedEntries() {
        var first = unsupported("x.scenario.json");
        var second = unsupported("y.scenario.json");
        var input = input(List.of(), List.of(first, second));
        var fingerprint = RepositoryCaptureFingerprintEncoder.fingerprint(input);

        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint, List.of(), List.of(first)));
        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint, List.of(),
                List.of(first, second, unsupported("z.scenario.json"))));
        assertThrows(IllegalArgumentException.class, () -> attest(input, fingerprint, List.of(),
                List.of(second, first)));
    }

    @Test
    void reportParentHasNoUnattestedConstructionPath() {
        assertTrue(Arrays.stream(RepositoryCaptureAttestation.class.getDeclaredConstructors())
                .allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers())));
        assertTrue(Arrays.stream(RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture.class
                        .getDeclaredConstructors())
                .allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers())));
        assertEquals(List.of(RepositoryCaptureAttestation.class), Arrays.stream(
                        RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture.class
                                .getDeclaredMethods())
                .filter(method -> method.getName().equals("verified"))
                .map(method -> method.getParameterTypes()[0])
                .toList());
    }

    private static RepositoryCaptureAttestation attest(
            RepositoryCaptureFingerprintInput input,
            RepositoryCaptureFingerprint fingerprint,
            List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> members,
            List<RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry> unsupported
    ) {
        return RepositoryCaptureAttestation.verified(
                SOURCE, SNAPSHOT, input, fingerprint, members, unsupported);
    }

    private static List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> references(
            RepositoryCaptureFingerprintInput input,
            RepositoryCaptureFingerprint fingerprint
    ) {
        return input.capturedMembers().stream().map(member ->
                new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                        SOURCE, SNAPSHOT, fingerprint, member.normalizedRepositoryRelativePath(),
                        member.rawByteLength(), member.rawMemberFingerprint())).toList();
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference changed(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference original,
            String path,
            java.math.BigInteger length,
            RawSourceMemberFingerprint fingerprint
    ) {
        return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                original.parentSourceId(), original.parentSnapshotId(), original.parentContentFingerprint(),
                path, length, fingerprint);
    }

    private static RepositoryCaptureFingerprintInput input(
            List<RepositoryCaptureFingerprintInput.CapturedMember> members,
            List<RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry> unsupported
    ) {
        return new RepositoryCaptureFingerprintInput(
                SOURCE, "qaip-source-snapshot-contract-v1", "qaip-scenario-authority-repository-json-v1",
                "scenario-authority-repository-discovery-v1", "scenario-authority-repository-path-v1",
                "unicode-code-point-order-v1", RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,
                members, unsupported);
    }

    private static RepositoryCaptureFingerprintInput.CapturedMember member(String path, String content) {
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new RepositoryCaptureFingerprintInput.CapturedMember(
                path, bytes.length, RawSourceMemberFingerprint.calculate(bytes));
    }

    private static RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry unsupported(String path) {
        return new RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry(
                path, "SYMBOLIC_LINK", "UNSUPPORTED_SYMBOLIC_LINK");
    }
}
