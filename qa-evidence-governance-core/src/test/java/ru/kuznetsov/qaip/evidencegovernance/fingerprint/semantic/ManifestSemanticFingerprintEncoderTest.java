package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManifestSemanticFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final ScenarioSemanticFingerprint SCENARIO_A = new ScenarioSemanticFingerprint(
            "scenario-authority-scenario-semantic-v1:"
                    + "e4018280025f4c5de3b26576fe5040c7baeceaca47570733ba1f519e49f10544");
    private static final ScenarioSemanticFingerprint SCENARIO_B = new ScenarioSemanticFingerprint(
            "scenario-authority-scenario-semantic-v1:"
                    + "d703dadf95dc6e681c91ddb91993e04743cb71e5f49bd7fd68265f3ec1b23657");
    private static final String EMPTY_BYTES =
            "000000000000002c51414950005343454e4152494f5f415554484f524954595f4d414e49464553545f53454d414e544943005631"
                    + "000000000000002c7363656e6172696f2d617574686f726974792d6d616e69666573742d73656d616e7469632d6331346e2d7631"
                    + "000000000000000a7368612d3235362d7631"
                    + "000000000000002c7363656e6172696f2d617574686f726974792d6d616e69666573742d73656d616e7469632d6331346e2d7631"
                    + "00000000000000066f7264657273"
                    + "0000000000000023716169702d7363656e6172696f2d617574686f726974792d6d616e69666573742d7631"
                    + "0000000000000003312e30"
                    + "0000000000000019716169702d7363656e6172696f2d6964656e746974792d7631"
                    + "000000000000002a7363656e6172696f2d617574686f726974792d736f757263652d6e6f726d616c697a6174696f6e2d7631"
                    + "0000000000000000";

    @Test
    void freezesIdentifiersAndEmptyAdmittedManifestGoldenVector() {
        assertEquals("QAIP\u0000SCENARIO_AUTHORITY_MANIFEST_SEMANTIC\u0000V1",
                ManifestSemanticFingerprintEncoder.DOMAIN);
        assertEquals("scenario-authority-manifest-semantic-c14n-v1",
                ManifestSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", ManifestSemanticFingerprintEncoder.DIGEST_IDENTIFIER);
        assertEquals("scenario-authority-manifest-semantic-v1",
                ManifestSemanticFingerprint.VALUE_IDENTIFIER);
        assertEquals(EMPTY_BYTES, HEX.formatHex(ManifestSemanticFingerprintEncoder.encode(input(List.of()))));
        assertEquals("scenario-authority-manifest-semantic-v1:"
                        + "c224afe00928cb7728dfe9ed3fcb756df571a4fd949869827e40ed81f90497b2",
                fingerprint(input(List.of())).value());
        assertEquals("000000000000002c51414950005343454e4152494f5f415554484f524954595f"
                        + "4d414e49464553545f53454d414e544943005631",
                EMPTY_BYTES.substring(0, 104));
    }

    @Test
    void oneAndMultipleScenarioGoldenVectorsPreserveAuthoredOrder() {
        assertEquals("scenario-authority-manifest-semantic-v1:"
                        + "ca1dabbbae469a270ec148bb76597edd130b939be3aa68f9f856bf6bd76435d2",
                fingerprint(input(List.of(SCENARIO_A))).value());
        assertEquals("scenario-authority-manifest-semantic-v1:"
                        + "79a7bde91b196440052db3d34dea38688c80796eadd239bf8351d3fbf77f1192",
                fingerprint(input(List.of(SCENARIO_A, SCENARIO_B))).value());
        assertEquals("scenario-authority-manifest-semantic-v1:"
                        + "4cf9f01bfd75ec343eb1089125ad32d7653fa289e64ac629d7e829dc34a472af",
                fingerprint(input(List.of(SCENARIO_B, SCENARIO_A))).value());
        assertNotEquals(fingerprint(input(List.of(SCENARIO_A, SCENARIO_B))),
                fingerprint(input(List.of(SCENARIO_B, SCENARIO_A))));
        assertEquals(fingerprint(input(List.of(SCENARIO_A))),
                ManifestSemanticFingerprintComposerV1.fingerprintAcceptedInput(input(List.of(SCENARIO_A))));
        assertEquals(fingerprint(input(List.of(SCENARIO_A, SCENARIO_B))),
                ManifestSemanticFingerprintComposerV1.fingerprintAcceptedInput(
                        input(List.of(SCENARIO_A, SCENARIO_B))));
        assertEquals(fingerprint(input(List.of(SCENARIO_B, SCENARIO_A))),
                ManifestSemanticFingerprintComposerV1.fingerprintAcceptedInput(
                        input(List.of(SCENARIO_B, SCENARIO_A))));
    }

    @Test
    void authorityAndScenarioSemanticContentChangeFingerprint() {
        assertNotEquals(fingerprint(input("orders", List.of(SCENARIO_A))),
                fingerprint(input("payments", List.of(SCENARIO_A))));
        assertNotEquals(fingerprint(input(List.of(SCENARIO_A))),
                fingerprint(input(List.of(SCENARIO_B))));
    }

    @Test
    void unsupportedV1FormatSchemaIdentityNormalizationAndSemanticVersionsAreRejected() {
        assertUnsupported("scenario-authority-manifest-semantic-c14n-v2",
                format(), schema(), identity(), normalization());
        assertUnsupported(current(), "qaip-scenario-authority-manifest-v2",
                schema(), identity(), normalization());
        assertUnsupported(current(), format(), "2.0", identity(), normalization());
        assertUnsupported(current(), format(), schema(), "qaip-scenario-identity-v2", normalization());
        assertUnsupported(current(), format(), schema(), identity(),
                "scenario-authority-source-normalization-v2");
    }

    @Test
    void syntaxOccurrenceDiagnosticsAndQualificationCannotEnterManifestSemanticInput() {
        List<String> excluded = List.of("path", "parent", "capture", "raw", "occurrence", "json",
                "diagnostic", "provenance", "qualification", "resolution", "duplicate");
        assertFalse(Arrays.stream(ManifestSemanticFingerprintInput.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .anyMatch(name -> excluded.stream().anyMatch(name::contains)));

        ManifestSyntaxObservation compact = new ManifestSyntaxObservation("a.scenario.json", 0, input(List.of()));
        ManifestSyntaxObservation formatted = new ManifestSyntaxObservation("b.scenario.json", 999,
                input(List.of()));
        assertEquals(fingerprint(compact.semanticInput()), fingerprint(formatted.semanticInput()));
    }

    @Test
    void inputAndEncodingAreImmutableAndRepeatedCalculationIsDeterministic() {
        List<ScenarioSemanticFingerprint> mutable = new ArrayList<>(List.of(SCENARIO_A));
        ManifestSemanticFingerprintInput input = input(mutable);
        byte[] first = ManifestSemanticFingerprintEncoder.encode(input);
        mutable.clear();

        assertEquals(List.of(SCENARIO_A), input.scenarioSemanticFingerprints());
        assertThrows(UnsupportedOperationException.class, input.scenarioSemanticFingerprints()::clear);
        assertArrayEquals(first, ManifestSemanticFingerprintEncoder.encode(input));
        assertEquals(fingerprint(input), fingerprint(input));
        first[0] = 0x7f;
        assertArrayEquals(ManifestSemanticFingerprintEncoder.encode(input),
                ManifestSemanticFingerprintEncoder.encode(input));
    }

    @Test
    void immutableValueRejectsMalformedOrNonLowercaseFingerprint() {
        assertThrows(IllegalArgumentException.class,
                () -> new ManifestSemanticFingerprint(ManifestSemanticFingerprint.VALUE_PREFIX + "A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new ManifestSemanticFingerprint("sha-256-v1:" + "a".repeat(64)));
    }

    private static ManifestSemanticFingerprintInput input(List<ScenarioSemanticFingerprint> scenarios) {
        return input("orders", scenarios);
    }

    private static ManifestSemanticFingerprintInput input(
            String authority,
            List<ScenarioSemanticFingerprint> scenarios
    ) {
        return new ManifestSemanticFingerprintInput(current(), authority, format(), schema(), identity(),
                normalization(), scenarios);
    }

    private static void assertUnsupported(String semanticVersion, String format, String schema,
                                          String identity, String normalization) {
        assertThrows(IllegalArgumentException.class, () -> new ManifestSemanticFingerprintInput(
                semanticVersion, "orders", format, schema, identity, normalization, List.of()));
    }

    private static ManifestSemanticFingerprint fingerprint(ManifestSemanticFingerprintInput input) {
        return ManifestSemanticFingerprintEncoder.fingerprint(input);
    }

    private static String current() { return ManifestSemanticFingerprintEncoder.ENCODING_IDENTIFIER; }
    private static String format() { return ManifestSemanticFingerprintEncoder.MANIFEST_FORMAT_IDENTIFIER; }
    private static String schema() { return ManifestSemanticFingerprintEncoder.SCHEMA_VERSION; }
    private static String identity() { return ManifestSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION; }
    private static String normalization() { return ManifestSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION; }

    private record ManifestSyntaxObservation(
            String repositoryRelativePath,
            int formattingVariant,
            ManifestSemanticFingerprintInput semanticInput
    ) { }
}
