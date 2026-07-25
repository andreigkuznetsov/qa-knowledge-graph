package ru.kuznetsov.qaip.core.importing.parsing;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParsingContractInvariantTest {
    @Test
    void rawProjectJsonPreservesEveryAcceptedRawValue() {
        assertThrows(NullPointerException.class, () -> new RawProjectJson(null));
        assertEquals("", new RawProjectJson("").value());
        assertEquals(" \t\r\n ", new RawProjectJson(" \t\r\n ").value());
        assertEquals("  text  ", new RawProjectJson("  text  ").value());
    }

    @Test
    void acceptedRequiresDocument() {
        assertThrows(NullPointerException.class, () -> new ProjectParseAccepted(null));
    }

    @Test
    void rejectedDefensivelyCopiesDeduplicatesAndSortsFindings() {
        assertThrows(NullPointerException.class, () -> new ProjectParseRejected(null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectParseRejected(List.of()));
        List<ProjectParseFinding> containingNull = new ArrayList<>();
        containingNull.add(null);
        assertThrows(NullPointerException.class, () -> new ProjectParseRejected(containingNull));

        ProjectParseFinding late = finding(ProjectParseFindingCode.TRAILING_JSON_CONTENT, 20, "late");
        ProjectParseFinding early = finding(ProjectParseFindingCode.MALFORMED_JSON, 2, "early");
        ProjectParseFinding withoutPosition = new ProjectParseFinding(
                ProjectParseFindingCode.DUPLICATE_JSON_MEMBER,
                new JsonInstanceLocation("/z"), Optional.empty(), "without position");
        List<ProjectParseFinding> callerList = new ArrayList<>(List.of(withoutPosition, late, early, early));
        List<ProjectParseFinding> originalOrder = List.copyOf(callerList);

        ProjectParseRejected rejected = new ProjectParseRejected(callerList);

        assertEquals(originalOrder, callerList);
        assertEquals(List.of(early, late, withoutPosition), rejected.findings());
        callerList.clear();
        assertEquals(List.of(early, late, withoutPosition), rejected.findings());
        assertThrows(UnsupportedOperationException.class, () -> rejected.findings().add(late));
    }

    @Test
    void findingEnforcesEveryComponentInvariant() {
        JsonInstanceLocation root = JsonInstanceLocation.ROOT;
        Optional<JsonSourcePosition> noPosition = Optional.empty();
        assertThrows(NullPointerException.class, () -> new ProjectParseFinding(null, root, noPosition, "message"));
        assertThrows(NullPointerException.class, () -> new ProjectParseFinding(
                ProjectParseFindingCode.MALFORMED_JSON, null, noPosition, "message"));
        assertThrows(NullPointerException.class, () -> new ProjectParseFinding(
                ProjectParseFindingCode.MALFORMED_JSON, root, null, "message"));
        assertThrows(NullPointerException.class, () -> new ProjectParseFinding(
                ProjectParseFindingCode.MALFORMED_JSON, root, noPosition, null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectParseFinding(
                ProjectParseFindingCode.MALFORMED_JSON, root, noPosition, ""));
        assertThrows(IllegalArgumentException.class, () -> new ProjectParseFinding(
                ProjectParseFindingCode.MALFORMED_JSON, root, noPosition, " \t"));
        assertEquals("valid", new ProjectParseFinding(ProjectParseFindingCode.MALFORMED_JSON,
                root, noPosition, "valid").message());
    }

    @Test
    void sourcePositionUsesOneBasedLineAndColumnAndZeroBasedOffset() {
        assertThrows(IllegalArgumentException.class, () -> new JsonSourcePosition(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new JsonSourcePosition(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new JsonSourcePosition(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new JsonSourcePosition(1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new JsonSourcePosition(1, 1, -1));
        assertEquals(new JsonSourcePosition(1, 1, 0), new JsonSourcePosition(1, 1, 0));
    }

    @Test
    void instanceLocationEnforcesOnlyRfc6901Syntax() {
        assertThrows(NullPointerException.class, () -> new JsonInstanceLocation(null));
        assertEquals("", new JsonInstanceLocation("").value());
        assertEquals("/nodes/0/id", new JsonInstanceLocation("/nodes/0/id").value());
        assertThrows(IllegalArgumentException.class, () -> new JsonInstanceLocation("nodes/0"));
        assertThrows(IllegalArgumentException.class, () -> new JsonInstanceLocation("/a~"));
        assertThrows(IllegalArgumentException.class, () -> new JsonInstanceLocation("/a~2b"));
        assertEquals("/a~0b", new JsonInstanceLocation("/a~0b").value());
        assertEquals("/a~1b", new JsonInstanceLocation("/a~1b").value());
        assertEquals("/~0~1", new JsonInstanceLocation("/~0~1").value());
        assertEquals("/a~01b", new JsonInstanceLocation("/a~01b").value());
    }

    private static ProjectParseFinding finding(ProjectParseFindingCode code, long offset, String message) {
        return new ProjectParseFinding(code, JsonInstanceLocation.ROOT,
                Optional.of(new JsonSourcePosition(1, offset + 1, offset)), message);
    }
}
