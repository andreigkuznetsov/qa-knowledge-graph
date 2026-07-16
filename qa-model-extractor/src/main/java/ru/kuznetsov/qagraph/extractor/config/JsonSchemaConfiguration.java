package ru.kuznetsov.qagraph.extractor.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.kuznetsov.qagraph.extractor.validation.JsonDocumentSchemaValidator;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class JsonSchemaConfiguration {

    @Bean
    @Qualifier("storyInputSchemaValidator")
    JsonDocumentSchemaValidator storyInputSchemaValidator(ObjectMapper objectMapper)
            throws IOException {
        return new JsonDocumentSchemaValidator(
                loadSchema(objectMapper, "schemas/story-input-v0.1.schema.json"),
                true
        );
    }

    @Bean
    @Qualifier("generatedQaModelSchemaValidator")
    JsonDocumentSchemaValidator generatedQaModelSchemaValidator(ObjectMapper objectMapper)
            throws IOException {
        return new JsonDocumentSchemaValidator(
                loadSchema(objectMapper, "schemas/qa-model-v0.1.schema.json"),
                false
        );
    }

    private JsonSchema loadSchema(ObjectMapper objectMapper, String path)
            throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012
        );

        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            JsonNode schemaNode = objectMapper.readTree(inputStream);
            return factory.getSchema(schemaNode);
        }
    }
}
