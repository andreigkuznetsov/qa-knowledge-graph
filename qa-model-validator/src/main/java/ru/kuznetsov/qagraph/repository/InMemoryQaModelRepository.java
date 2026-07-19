package ru.kuznetsov.qagraph.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryQaModelRepository {

    private final ConcurrentMap<String, JsonNode> models =
            new ConcurrentHashMap<>();

    public String save(JsonNode model) {
        String modelId = UUID.randomUUID().toString();
        models.put(modelId, model.deepCopy());
        return modelId;
    }

    public Optional<JsonNode> findById(String modelId) {
        return Optional.ofNullable(models.get(modelId))
                .map(JsonNode::deepCopy);
    }
}
