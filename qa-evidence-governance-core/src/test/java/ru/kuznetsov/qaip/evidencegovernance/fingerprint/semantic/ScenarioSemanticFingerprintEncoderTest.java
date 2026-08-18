package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioSemanticFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final String GOLDEN_BYTES =
            "000000000000002c51414950005343454e4152494f5f415554484f524954595f5343454e4152494f5f53454d414e544943005631"
            + "000000000000002c7363656e6172696f2d617574686f726974792d7363656e6172696f2d73656d616e7469632d6331346e2d7631"
            + "000000000000000a7368612d3235362d7631"
            + "000000000000002c7363656e6172696f2d617574686f726974792d7363656e6172696f2d73656d616e7469632d6331346e2d7631"
            + "00000000000000066f726465727300000000000000064352454154450000000000000019716169702d7363656e6172696f2d6964656e746974792d7631"
            + "000000000000000c437265617465206f72646572"
            + "0000000000000002"
            + "00000000000000237363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d763100000000000000647363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d76313a36623961313961666632306231356439333539373735346535316630373764383938313562393235366539613561353564303264393164396333333233343065"
            + "00000000000000237363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d763100000000000000647363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d76313a62326135306437373862636136373737383763363235623664383734616163396164393966356136313063323163653836306566383761653065643038646365"
            + "0000000000000001"
            + "00000000000000237363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d763100000000000000647363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d76313a30366432353338336564386265336531376138363035326465633265363431636163323761346439363832653632616262643630356465613235306331333636"
            + "0000000000000001"
            + "00000000000000237363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d763100000000000000647363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d76313a30656637633737373235396262656134373830373666626430313134623064393464323262303333656331623830393239326631613162306535343561333161"
            + "00000000000000377363656e6172696f2d617574686f726974792d687474702d6f7065726174696f6e2d7265666572656e63652d73656d616e7469632d7631"
            + "00000000000000787363656e6172696f2d617574686f726974792d687474702d6f7065726174696f6e2d7265666572656e63652d73656d616e7469632d76313a33323062653139356132616632373264633834656236643962356163656464616638646438303035633035616633346336353431396465656537376530336339"
            + "0000000000000002"
            + "00000000000000367363656e6172696f2d617574686f726974792d627573696e6573732d72756c652d7265666572656e63652d73656d616e7469632d763100000000000000777363656e6172696f2d617574686f726974792d627573696e6573732d72756c652d7265666572656e63652d73656d616e7469632d76313a65663939333861653539393731356163346561636539626337646563316565396234353935373634333632663735316131623765303862373638326561383839"
            + "00000000000000367363656e6172696f2d617574686f726974792d627573696e6573732d72756c652d7265666572656e63652d73656d616e7469632d763100000000000000777363656e6172696f2d617574686f726974792d627573696e6573732d72756c652d7265666572656e63652d73656d616e7469632d76313a39313664316235363930333230373364626165643436373831306635646538363538393032663432623632343837323965666130323930613465373230613136"
            + "000000000000002a7363656e6172696f2d617574686f726974792d736f757263652d6e6f726d616c697a6174696f6e2d7631"
            + "000000000000002c7363656e6172696f2d617574686f726974792d7363656e6172696f2d73656d616e7469632d6331346e2d7631";

    @Test
    void freezesIdentifiersAndCompleteGoldenVector() {
        assertEquals("QAIP\u0000SCENARIO_AUTHORITY_SCENARIO_SEMANTIC\u0000V1",
                ScenarioSemanticFingerprintEncoder.DOMAIN);
        assertEquals("scenario-authority-scenario-semantic-c14n-v1",
                ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", ScenarioSemanticFingerprintEncoder.DIGEST_IDENTIFIER);
        assertEquals("scenario-authority-scenario-semantic-v1",
                ScenarioSemanticFingerprint.VALUE_IDENTIFIER);
        assertEquals(GOLDEN_BYTES, HEX.formatHex(ScenarioSemanticFingerprintEncoder.encode(base())));
        assertEquals("scenario-authority-scenario-semantic-v1:"
                        + "e4018280025f4c5de3b26576fe5040c7baeceaca47570733ba1f519e49f10544",
                ScenarioSemanticFingerprintEncoder.fingerprint(base()).value());
        assertEquals("000000000000002c51414950005343454e4152494f5f415554484f524954595f"
                        + "5343454e4152494f5f53454d414e544943005631",
                GOLDEN_BYTES.substring(0, 104));
    }

    @Test
    void titleAndClaimedIdentityAreSemanticButOccurrenceIsExcluded() {
        assertDifferent(base(), withTitle("Create a new order"));
        assertDifferent(base(), withIdentity("payments", "CREATE"));
        ScenarioOccurrence first = new ScenarioOccurrence("manifest-a", 0, base());
        ScenarioOccurrence later = new ScenarioOccurrence("manifest-b", 7, base());
        assertEquals(fingerprint(first.semanticInput()), fingerprint(later.semanticInput()));
        assertFalse(Arrays.stream(ScenarioSemanticFingerprintInput.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("occurrence")));
    }

    @Test
    void stepTextOrderAndPhaseChangesAreSemantic() {
        ScenarioSemanticFingerprintInput changedText = input("orders", "CREATE", "Create order",
                List.of(step("GIVEN", 0, "a different customer"), step("GIVEN", 1, "balance is available")),
                base().whenStepFingerprints(), base().thenStepFingerprints(), operation("POST", "/api/orders"), rules());
        ScenarioSemanticFingerprintInput reordered = input("orders", "CREATE", "Create order",
                List.of(step("GIVEN", 1, "balance is available"), step("GIVEN", 0, "a customer exists")),
                base().whenStepFingerprints(), base().thenStepFingerprints(), operation("POST", "/api/orders"), rules());
        ScenarioSemanticFingerprintInput phaseChanged = input("orders", "CREATE", "Create order",
                List.of(step("GIVEN", 0, "a customer exists")),
                List.of(step("GIVEN", 1, "balance is available"), step("WHEN", 0, "the customer submits an order")),
                base().thenStepFingerprints(), operation("POST", "/api/orders"), rules());
        assertDifferent(base(), changedText);
        assertDifferent(base(), reordered);
        assertDifferent(base(), phaseChanged);
    }

    @Test
    void operationAndOrderedRuleReferencesAreSemantic() {
        assertDifferent(base(), input("orders", "CREATE", "Create order", base().givenStepFingerprints(),
                base().whenStepFingerprints(), base().thenStepFingerprints(),
                operation("PUT", "/api/orders/1"), rules()));
        assertDifferent(base(), input("orders", "CREATE", "Create order", base().givenStepFingerprints(),
                base().whenStepFingerprints(), base().thenStepFingerprints(), operation("POST", "/api/orders"),
                List.of(rule("policy", "order.minimum"), rule("fraud", "order.review"))));
        assertDifferent(base(), input("orders", "CREATE", "Create order", base().givenStepFingerprints(),
                base().whenStepFingerprints(), base().thenStepFingerprints(), operation("POST", "/api/orders"),
                List.of(rule("fraud", "order.review"), rule("policy", "order.limit"))));
    }

    @Test
    void unicodeDelimiterLikeAuthoredMeaningAndRepeatedCalculationAreDeterministic() {
        ScenarioSemanticFingerprintInput unicode = input("orders", "CREATE", "Заказ | готов\u0000",
                List.of(step("GIVEN", 0, "клиент | существует\u0000")), List.of(), List.of(),
                operation("POST", "/api/заказы|special\u0000tail"), List.of());
        assertEquals("scenario-authority-scenario-semantic-v1:"
                        + "d703dadf95dc6e681c91ddb91993e04743cb71e5f49bd7fd68265f3ec1b23657",
                fingerprint(unicode).value());
        assertArrayEquals(ScenarioSemanticFingerprintEncoder.encode(unicode),
                ScenarioSemanticFingerprintEncoder.encode(unicode));
        assertEquals(fingerprint(unicode), fingerprint(unicode));
    }

    @Test
    void exactAuthoredValuesAndOrderedCollectionsAreDefensivelyPreserved() {
        assertDifferent(withTitle("Create order"), withTitle(" Create order "));
        List<StepSemanticFingerprint> mutable = new ArrayList<>(base().givenStepFingerprints());
        ScenarioSemanticFingerprintInput captured = input("orders", "CREATE", "Create order", mutable,
                base().whenStepFingerprints(), base().thenStepFingerprints(), operation("POST", "/api/orders"), rules());
        ScenarioSemanticFingerprint before = fingerprint(captured);
        mutable.clear();
        assertEquals(before, fingerprint(captured));
        assertThrows(UnsupportedOperationException.class, captured.givenStepFingerprints()::clear);
    }

    @Test
    void v1BoundaryRejectsUnsupportedIdentityNormalizationAndSemanticVersions() {
        assertThrows(IllegalArgumentException.class, () -> invalidVersion(
                "qaip-scenario-identity-v2", current(), normalization(), current()));
        assertThrows(IllegalArgumentException.class, () -> invalidVersion(
                identity(), "scenario-authority-scenario-semantic-c14n-v2", normalization(), current()));
        assertThrows(IllegalArgumentException.class, () -> invalidVersion(
                identity(), current(), "scenario-authority-source-normalization-v2", current()));
        assertThrows(IllegalArgumentException.class, () -> invalidVersion(
                identity(), current(), normalization(), "scenario-authority-scenario-semantic-c14n-v2"));
        assertThrows(IllegalArgumentException.class,
                () -> new ScenarioSemanticFingerprint(ScenarioSemanticFingerprint.VALUE_PREFIX + "A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new StepSemanticFingerprint("scenario-authority-step-semantic-v2:" + "a".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new HttpOperationReferenceSemanticFingerprint(
                        "scenario-authority-http-operation-reference-semantic-v2:" + "a".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new BusinessRuleReferenceSemanticFingerprint(
                        "scenario-authority-business-rule-reference-semantic-v2:" + "a".repeat(64)));
    }

    private static ScenarioSemanticFingerprintInput base() {
        return input("orders", "CREATE", "Create order",
                List.of(step("GIVEN", 0, "a customer exists"), step("GIVEN", 1, "balance is available")),
                List.of(step("WHEN", 0, "the customer submits an order")),
                List.of(step("THEN", 0, "the order is accepted")),
                operation("POST", "/api/orders"), rules());
    }

    private static ScenarioSemanticFingerprintInput withTitle(String title) {
        return input("orders", "CREATE", title, base().givenStepFingerprints(),
                base().whenStepFingerprints(), base().thenStepFingerprints(), operation("POST", "/api/orders"), rules());
    }

    private static ScenarioSemanticFingerprintInput withIdentity(String authority, String key) {
        return input(authority, key, "Create order", base().givenStepFingerprints(),
                base().whenStepFingerprints(), base().thenStepFingerprints(), operation("POST", "/api/orders"), rules());
    }

    private static ScenarioSemanticFingerprintInput input(String authority, String key, String title,
                                                           List<StepSemanticFingerprint> given,
                                                           List<StepSemanticFingerprint> when,
                                                           List<StepSemanticFingerprint> then,
                                                           HttpOperationReferenceSemanticFingerprint operation,
                                                           List<BusinessRuleReferenceSemanticFingerprint> rules) {
        return new ScenarioSemanticFingerprintInput(authority, key, identity(), title, given, when, then,
                operation, rules, current(), normalization(), current());
    }

    private static ScenarioSemanticFingerprintInput invalidVersion(String identityVersion,
                                                                    String canonicalizationVersion,
                                                                    String normalizationVersion,
                                                                    String semanticContractVersion) {
        return new ScenarioSemanticFingerprintInput("orders", "CREATE", identityVersion, "Create order",
                List.of(), List.of(), List.of(), operation("POST", "/api/orders"), List.of(),
                canonicalizationVersion, normalizationVersion, semanticContractVersion);
    }

    private static StepSemanticFingerprint step(String phase, long ordinal, String text) {
        return StepSemanticFingerprintEncoder.fingerprint(new StepSemanticFingerprintInput(
                "orders", "CREATE", identity(), StepSemanticFingerprintInput.Phase.valueOf(phase),
                BigInteger.valueOf(ordinal), "qaip-scenario-step-identity-v1", text,
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    private static HttpOperationReferenceSemanticFingerprint operation(String method, String path) {
        return HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(
                new HttpOperationReferenceSemanticFingerprintInput("orders", "CREATE", identity(),
                        "OPERATION_REF", "qaip-scenario-operation-reference-datum-identity-v1",
                        "qaip-http-operation-reference-v1", method, path,
                        HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    private static BusinessRuleReferenceSemanticFingerprint rule(String authority, String key) {
        return BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(
                new BusinessRuleReferenceSemanticFingerprintInput("orders", "CREATE", identity(), authority, key,
                        "qaip-business-rule-identity-v1",
                        "qaip-scenario-business-rule-reference-datum-identity-v1",
                        BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    private static List<BusinessRuleReferenceSemanticFingerprint> rules() {
        return List.of(rule("policy", "order.limit"), rule("fraud", "order.review"));
    }

    private static ScenarioSemanticFingerprint fingerprint(ScenarioSemanticFingerprintInput input) {
        return ScenarioSemanticFingerprintEncoder.fingerprint(input);
    }

    private static void assertDifferent(ScenarioSemanticFingerprintInput left,
                                        ScenarioSemanticFingerprintInput right) {
        assertNotEquals(fingerprint(left), fingerprint(right));
    }

    private static String identity() { return ScenarioSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION; }
    private static String current() { return ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER; }
    private static String normalization() { return ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION; }

    private record ScenarioOccurrence(String path, int scenarioIndex,
                                      ScenarioSemanticFingerprintInput semanticInput) { }
}
