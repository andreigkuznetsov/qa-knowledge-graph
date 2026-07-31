package ru.kuznetsov.qaip.core.persistence.document;

import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PersistenceDocumentFixtures {
    private PersistenceDocumentFixtures() { }

    static Project completeProject() {
        Map<String, Object> dynamic = allDynamicValues();
        Metadata metadata = new Metadata("P-1", "Project", null, "", dynamic);
        Map<String, Object> source = map("id", "source-1", "rank", Long.MAX_VALUE, "nullable", null);
        Node node = new Node("N-1", "BUSINESS_RULE", "Rule", null, "", List.of("one", "two"),
                List.of(map("sourceId", "source-1", "confidence", new BigDecimal("1.00"))),
                map("nodeMeta", BigInteger.TEN), map("rule", map("code", "BR-1", "active", true)));
        Relationship relationship = new Relationship("R-1", "N-1", "DEPENDS_ON", "N-2",
                map("weight", Integer.valueOf(1)), List.of(source));
        EvidenceManifest evidence = new EvidenceManifest("impact-evidence-manifest-v1", "source-1",
                map("snapshotId", "snap", "ordinal", Long.valueOf(1)),
                "normalization-v1", "canonicalization-v1", "fingerprint",
                List.of(map("assertionId", "A-1", "resolved", true)),
                List.of(map("datumId", "D-1", "score", new BigDecimal("0.100"))),
                List.of(map("provenanceId", "PR-1", "sequence", BigInteger.ONE)));
        DeclaredChange change = new DeclaredChange("NODE", "N-1", "MODIFIED", "0.1",
                map("id", "N-1", "count", Integer.MIN_VALUE),
                map("id", "N-1", "count", Integer.MAX_VALUE));
        return new Project("qaip-project-v1", "0.1", metadata, List.of(source), new Subject("N-1"),
                List.of(node), List.of(relationship), evidence, List.of(change),
                Map.of("algorithmVersion", "v1", "empty", ""));
    }

    static Project minimalProject() {
        return new Project("qaip-project-v1", "0.1", new Metadata("P-0", "", null, null, Map.of()),
                List.of(), new Subject("local"), List.of(), List.of(),
                new EvidenceManifest("manifest-v1", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()),
                List.of(), Map.of());
    }

    static Map<String, Object> allDynamicValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("null", null);
        values.put("emptyString", "");
        values.put("string", "value");
        values.put("true", true);
        values.put("false", false);
        values.put("intMin", Integer.MIN_VALUE);
        values.put("intMax", Integer.MAX_VALUE);
        values.put("longMin", Long.MIN_VALUE);
        values.put("longMax", Long.MAX_VALUE);
        values.put("bigZero", BigInteger.ZERO);
        values.put("bigPositive", new BigInteger("1234567890123456789012345678901234567890"));
        values.put("bigNegative", new BigInteger("-1234567890123456789012345678901234567890"));
        values.put("decimalZero", BigDecimal.ZERO);
        values.put("decimal1", new BigDecimal("1"));
        values.put("decimal10", new BigDecimal("1.0"));
        values.put("decimal100", new BigDecimal("1.00"));
        values.put("decimalNegative", new BigDecimal("-10.2500"));
        values.put("decimalPrecise", new BigDecimal("1234567890.123456789012345678900"));
        values.put("decimalNegativeScale", new BigDecimal(BigInteger.valueOf(123), -5));
        values.put("decimalMaxScale", new BigDecimal(BigInteger.ZERO, Integer.MAX_VALUE));
        values.put("emptyList", List.of());
        values.put("list", listWithNull("first", Integer.valueOf(1), map("nested", Long.valueOf(2))));
        values.put("emptyObject", Map.of());
        values.put("nested", map("presentNull", null, "deep", List.of(map("value", BigInteger.TEN))));
        return values;
    }

    static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) result.put((String) values[index], values[index + 1]);
        return result;
    }

    static List<Object> listWithNull(Object... values) {
        List<Object> result = new ArrayList<>();
        for (Object value : values) result.add(value);
        result.add(1, null);
        return result;
    }
}
