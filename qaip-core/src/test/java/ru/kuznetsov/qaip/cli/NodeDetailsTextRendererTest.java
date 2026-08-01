package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsResult;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsTextRendererTest {
    private final NodeDetailsTextRenderer renderer = new NodeDetailsTextRenderer();

    @Test
    void renders_exact_fields_in_order_and_preserves_non_null_text() {
        NodeDetailsResult details = new NodeDetailsResult(
                " N ", " Type_Value ", " Name ", " Description ", " Status ");
        String expected = String.join(System.lineSeparator(), "Node Details", "Project ID:  P ",
                "Node ID:  N ", "Type:  Type_Value ", "Name:  Name ",
                "Description:  Description ", "Status:  Status ");
        assertEquals(expected, renderer.renderFound(" P ", details));
        assertEquals(expected, renderer.renderFound(" P ", details));
        assertFalse(expected.contains("NodeDetailsResult["));
    }

    @Test
    void renders_only_absent_optional_values_as_not_specified() {
        String rendered = renderer.renderFound("P",
                new NodeDetailsResult("N", "CHECK", "name", null, null));
        assertTrue(rendered.endsWith("Description: not specified" + System.lineSeparator()
                + "Status: not specified"));
    }
}
