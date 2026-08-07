package ru.kuznetsov.qaip.core.application.query.operationoverview;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.operationlist.DefaultOperationListQuery;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListFound;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DefaultOperationOverviewQueryTest {
    @Test
    void returns_project_not_found_after_exactly_one_read() {
        CountingProjectReader reader = new CountingProjectReader(Optional.empty());

        var result = new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1");

        assertEquals(new OperationOverviewProjectNotFound("P-1"), result);
        assertEquals(1, reader.readCount());
    }

    @Test
    void returns_operation_not_found_from_the_single_snapshot() {
        CountingProjectReader reader = new CountingProjectReader(Optional.of(project(operation("OP-OTHER"))));

        var result = new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1");

        assertEquals(new OperationOverviewOperationNotFound("P-1", "OP-1"), result);
        assertEquals(1, reader.readCount());
    }

    @Test
    void resolves_identity_and_transitional_sections_from_one_snapshot() {
        CountingProjectReader reader = new CountingProjectReader(Optional.of(project(operation("OP-1"))));

        OperationOverviewFound found = assertInstanceOf(OperationOverviewFound.class,
                new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1"));

        assertEquals(new OperationOverviewIdentity(
                "P-1", "OP-1", "POST", "/orders", "POST /orders"), found.identity());
        assertInstanceOf(OperationOverviewImplementationIncomplete.class, found.implementation());
        assertInstanceOf(OperationOverviewEventPathNotApplicable.class, found.eventPath());
        OperationOverviewVerificationAvailable verification = assertInstanceOf(
                OperationOverviewVerificationAvailable.class, found.verification());
        assertEquals(0, verification.testCount());
        assertEquals(0, verification.checkCount());
        assertEquals(List.of(), verification.tests());
        assertEquals(1, reader.readCount());
    }

    @Test
    void repeated_resolution_is_deterministic() {
        Project snapshot = project(operation("OP-1"));
        OperationOverviewQuery query = new DefaultOperationOverviewQuery(id -> Optional.of(snapshot));

        OperationOverviewQueryResult first = query.execute("P-1", "OP-1");
        OperationOverviewQueryResult second = query.execute("P-1", "OP-1");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void identity_values_match_the_existing_operation_list_semantics() {
        Project snapshot = project(operation("OP-1"));
        ProjectReader reader = id -> Optional.of(snapshot);
        OperationOverviewFound overview = assertInstanceOf(OperationOverviewFound.class,
                new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1"));
        OperationListFound list = assertInstanceOf(OperationListFound.class,
                new DefaultOperationListQuery(reader, new OperationListProjector()).execute("P-1"));
        var existing = list.operations().getFirst();

        assertEquals(existing.operationId(), overview.identity().operationId());
        assertEquals(existing.method(), overview.identity().method());
        assertEquals(existing.path(), overview.identity().path());
        assertEquals(existing.displayName(), overview.identity().displayName());
    }

    private static Project project(Node operation) {
        return new Project("contract", "schema", new Metadata("P-1", "Project", null, null, Map.of()),
                List.of(), new Subject(operation.id()), List.of(operation), List.of(),
                new EvidenceManifest("evidence", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static Node operation(String id) {
        return new Node(id, "BUSINESS_OPERATION", "POST /orders", null, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of("operation", Map.of("code", id)));
    }

    private static final class CountingProjectReader implements ProjectReader {
        private final Optional<Project> result;
        private int readCount;

        private CountingProjectReader(Optional<Project> result) {
            this.result = result;
        }

        @Override
        public Optional<Project> findById(String projectId) {
            readCount++;
            return result;
        }

        private int readCount() {
            return readCount;
        }
    }
}
