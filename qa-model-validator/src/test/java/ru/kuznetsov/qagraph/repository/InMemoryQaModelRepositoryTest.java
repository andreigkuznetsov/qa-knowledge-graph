package ru.kuznetsov.qagraph.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryQaModelRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InMemoryQaModelRepository repository =
            new InMemoryQaModelRepository();

    @Test
    void shouldSaveAndReturnDefensiveCopy() throws Exception {
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );

        String modelId = repository.save(model, 0).modelId();
        JsonNode stored = repository.findById(modelId).orElseThrow();

        ((com.fasterxml.jackson.databind.node.ObjectNode) stored)
                .put("changed", true);

        assertEquals(model, repository.findById(modelId).orElseThrow());
    }

    @Test
    void shouldGenerateDifferentIdentifiers() throws Exception {
        JsonNode model = objectMapper.readTree("{}");

        assertNotEquals(
                repository.save(model, 0).modelId(),
                repository.save(model, 0).modelId()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownIdentifier() {
        assertTrue(repository.findById("unknown").isEmpty());
        assertTrue(repository.findDescriptorById("unknown").isEmpty());
    }

    @Test
    void shouldCreateDescriptorAndSortNewestFirst() throws Exception {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(
                Instant.parse("2026-07-19T10:00:00Z"),
                Instant.parse("2026-07-19T11:00:00Z")
        );
        InMemoryQaModelRepository timedRepository =
                new InMemoryQaModelRepository(clock);
        JsonNode firstModel = objectMapper.readTree(
                "{\"nodes\":[{}],\"relationships\":[]}"
        );
        JsonNode secondModel = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[{},{}]}"
        );

        var first = timedRepository.save(firstModel, 1);
        var second = timedRepository.save(secondModel, 2);
        List<ru.kuznetsov.qagraph.registration.ModelDescriptor> descriptors =
                timedRepository.findAllDescriptors();

        assertEquals(List.of(second, first), descriptors);
        assertEquals(0, second.nodeCount());
        assertEquals(2, second.relationshipCount());
        assertEquals(2, second.warningCount());
        assertEquals(
                second,
                timedRepository.findDescriptorById(second.modelId())
                        .orElseThrow()
        );
    }
}
