package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.api.TraceNodeElement;
import ru.kuznetsov.qagraph.api.TracePathElement;
import ru.kuznetsov.qagraph.api.TraceRelationshipElement;
import ru.kuznetsov.qagraph.api.TraceResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.trace.TracePath;
import ru.kuznetsov.qagraph.trace.TraceEngine;

import java.util.ArrayList;
import java.util.List;

@Service
public class QaModelTraceService {

    private final InMemoryQaModelRepository repository;
    private final TraceEngine traceEngine;

    public QaModelTraceService(
            InMemoryQaModelRepository repository,
            TraceEngine traceEngine
    ) {
        this.repository = repository;
        this.traceEngine = traceEngine;
    }

    public TraceResponse trace(
            String modelId,
            String fromNodeId,
            String toNodeId
    ) {
        requireParameter("from", fromNodeId);
        requireParameter("to", toNodeId);

        JsonNode model = repository.findById(modelId)
                .orElseThrow(() ->
                        new QaModelNotFoundException(modelId)
                );
        TracePath tracePath = traceEngine.trace(
                model,
                fromNodeId,
                toNodeId
        );

        return new TraceResponse(
                modelId,
                fromNodeId,
                toNodeId,
                tracePath.found(),
                tracePath.relationships().size(),
                toElements(tracePath)
        );
    }

    private List<TracePathElement> toElements(TracePath tracePath) {
        if (!tracePath.found()) {
            return List.of();
        }

        List<TracePathElement> elements = new ArrayList<>();

        for (int index = 0; index < tracePath.nodes().size(); index++) {
            var node = tracePath.nodes().get(index);
            elements.add(new TraceNodeElement(
                    node.id(),
                    node.type(),
                    node.name()
            ));

            if (index < tracePath.relationships().size()) {
                var relationship =
                        tracePath.relationships().get(index);
                elements.add(new TraceRelationshipElement(
                        relationship.type(),
                        relationship.from(),
                        relationship.to()
                ));
            }
        }

        return elements;
    }

    private void requireParameter(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestParameterException(name);
        }
    }
}
