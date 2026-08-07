package ru.kuznetsov.qaip.core.application.query.operationlist;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Node;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationIdentityResolverTest {
    private final OperationIdentityResolver resolver = new OperationIdentityResolver();

    @Test
    void resolves_complete_identity_with_canonical_method_and_path_parsing() {
        var identity = resolver.resolve(operation("OP-1", "  POST   /api/orders  "));

        assertEquals("OP-1", identity.operationId());
        assertEquals("POST", identity.method());
        assertEquals("/api/orders", identity.path());
        assertEquals("  POST   /api/orders  ", identity.displayName());
    }

    @Test
    void resolution_is_deterministic_and_rejects_invalid_display_names() {
        Node operation = operation("OP-1", "GET /");

        assertEquals(resolver.resolve(operation), resolver.resolve(operation));
        assertThrows(NullPointerException.class, () -> resolver.resolve(null));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(operation("OP-1", "GET")));
    }

    private static Node operation(String id, String name) {
        return new Node(id, "BUSINESS_OPERATION", name, null, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of());
    }
}
