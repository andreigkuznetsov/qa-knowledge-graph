package ru.kuznetsov.qagraph.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Repository;
import ru.kuznetsov.qagraph.registration.ModelDescriptor;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryQaModelRepository {

    private final ConcurrentMap<String, RegisteredModel> models =
            new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryQaModelRepository() {
        this(Clock.systemUTC());
    }

    InMemoryQaModelRepository(Clock clock) {
        this.clock = clock;
    }

    public ModelDescriptor save(JsonNode model, int warningCount) {
        String modelId = UUID.randomUUID().toString();
        ModelDescriptor descriptor = new ModelDescriptor(
                modelId,
                Instant.now(clock),
                model.path("nodes").size(),
                model.path("relationships").size(),
                warningCount
        );
        models.put(
                modelId,
                new RegisteredModel(model.deepCopy(), descriptor)
        );
        return descriptor;
    }

    public Optional<JsonNode> findById(String modelId) {
        return Optional.ofNullable(models.get(modelId))
                .map(RegisteredModel::model)
                .map(JsonNode::deepCopy);
    }

    public Optional<ModelDescriptor> findDescriptorById(String modelId) {
        return Optional.ofNullable(models.get(modelId))
                .map(RegisteredModel::descriptor);
    }

    public List<ModelDescriptor> findAllDescriptors() {
        return models.values().stream()
                .map(RegisteredModel::descriptor)
                .sorted(Comparator
                        .comparing(ModelDescriptor::createdAt)
                        .reversed()
                        .thenComparing(ModelDescriptor::modelId))
                .toList();
    }

    private record RegisteredModel(
            JsonNode model,
            ModelDescriptor descriptor
    ) {
    }
}
