package ru.kuznetsov.qaip.simulation.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationContractsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void taskMaterializationShouldDeepCopyOnIngressAndEgress() {
        var original = mapper.createObjectNode().put("id", "SC-NEW");
        var materialization = new TaskMaterialization("TASK-1", original);
        original.put("id", "CHANGED");

        assertEquals("SC-NEW", materialization.futureNode().path("id").asText());
        var exposed = materialization.futureNode();
        ((com.fasterxml.jackson.databind.node.ObjectNode) exposed)
                .put("id", "ALSO-CHANGED");
        assertEquals("SC-NEW", materialization.futureNode().path("id").asText());
    }

    @Test
    void taskMaterializationShouldRejectInvalidRequiredValues() {
        var node = mapper.createObjectNode();
        assertThrows(NullPointerException.class,
                () -> new TaskMaterialization(null, node));
        assertThrows(IllegalArgumentException.class,
                () -> new TaskMaterialization(" ", node));
        assertThrows(NullPointerException.class,
                () -> new TaskMaterialization("T", null));
        assertThrows(IllegalArgumentException.class,
                () -> new TaskMaterialization("T", mapper.createArrayNode()));
    }

    @Test
    void materializationSetShouldCopyAndValidateCollection() {
        var item = new TaskMaterialization("T", mapper.createObjectNode());
        var input = new ArrayList<>(List.of(item));
        var set = new TaskMaterializationSet("0.1", "fingerprint", input);
        input.clear();

        assertEquals(1, set.materializations().size());
        assertThrows(UnsupportedOperationException.class,
                () -> set.materializations().clear());
        assertThrows(NullPointerException.class,
                () -> new TaskMaterializationSet("0.1", "f", null));
        assertThrows(NullPointerException.class,
                () -> new TaskMaterializationSet("0.1", "f",
                        Arrays.asList(item, null)));
        assertThrows(IllegalArgumentException.class,
                () -> new TaskMaterializationSet(" ", "f", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new TaskMaterializationSet("0.1", " ", List.of()));
    }

    @Test
    void simulationResultShouldDeepCopyModelAndCopyList() {
        var model = mapper.createObjectNode().put("schemaVersion", "0.1");
        var applied = new ArrayList<>(List.of(
                new AppliedMaterialization("T", "N", "R")));
        var validation = new QaModelValidationResult(
                true, "0.1", new ValidationSummary(0, 0, 0), List.of());
        var result = new SimulationResult(
                "0.1", "base", "future", model, applied, validation);
        model.put("schemaVersion", "changed");
        applied.clear();

        assertEquals("0.1", result.futureModel()
                .path("schemaVersion").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode) result.futureModel())
                .put("schemaVersion", "changed-again");
        assertEquals("0.1", result.futureModel()
                .path("schemaVersion").asText());
        assertEquals(1, result.appliedMaterializations().size());
        assertThrows(UnsupportedOperationException.class,
                () -> result.appliedMaterializations().clear());
        assertEquals(validation, result.validation());
    }

    @Test
    void simulationResultShouldHaveNoSimulatedComponent() {
        assertFalse(Arrays.stream(SimulationResult.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("simulated")));
        assertEquals(List.of(
                        "simulationContractVersion", "baseModelFingerprint",
                        "futureModelFingerprint", "futureModel",
                        "appliedMaterializations", "validation"),
                Arrays.stream(SimulationResult.class.getRecordComponents())
                        .map(component -> component.getName()).toList());
    }
}
