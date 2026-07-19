package ru.kuznetsov.qagraph.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryQaModelRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InMemoryQaModelRepository repository =
            new InMemoryQaModelRepository();

    @Test
    void shouldSaveAndReturnDefensiveCopy() throws Exception {
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );

        String modelId = repository.save(model);
        JsonNode stored = repository.findById(modelId).orElseThrow();

        ((com.fasterxml.jackson.databind.node.ObjectNode) stored)
                .put("changed", true);

        assertEquals(model, repository.findById(modelId).orElseThrow());
    }

    @Test
    void shouldGenerateDifferentIdentifiers() throws Exception {
        JsonNode model = objectMapper.readTree("{}");

        assertNotEquals(repository.save(model), repository.save(model));
    }

    @Test
    void shouldReturnEmptyForUnknownIdentifier() {
        assertTrue(repository.findById("unknown").isEmpty());
    }
}
