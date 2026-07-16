package ru.kuznetsov.qagraph.extractor.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryInputToQaModelMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StoryInputToQaModelMapper mapper =
            new StoryInputToQaModelMapper(objectMapper);

    @Test
    void shouldCreateCoreGraphRelationships() throws Exception {
        JsonNode input;

        try (InputStream stream =
                     new ClassPathResource(
                             "story-input-sort-385.json"
                     ).getInputStream()) {
            input = objectMapper.readTree(stream);
        }

        StoryInputToQaModelMapper.MappingResult result = mapper.map(input);

        assertEquals("0.1", result.qaModel().path("schemaVersion").asText());

        assertTrue(result.qaModel().path("relationships")
                .findValuesAsText("type")
                .contains("DESCRIBES"));

        assertTrue(result.qaModel().path("relationships")
                .findValuesAsText("type")
                .contains("SPECIFIED_BY"));

        assertTrue(result.qaModel().path("relationships")
                .findValuesAsText("type")
                .contains("COVERS"));
    }
}
