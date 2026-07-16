package ru.kuznetsov.qagraph.extractor.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.kuznetsov.qagraph.extractor.mapping.StoryInputToQaModelMapper;
import ru.kuznetsov.qagraph.extractor.model.ExtractionIssue;
import ru.kuznetsov.qagraph.extractor.model.ExtractionSeverity;
import ru.kuznetsov.qagraph.extractor.model.ExtractionSummary;
import ru.kuznetsov.qagraph.extractor.model.QaModelExtractionResponse;
import ru.kuznetsov.qagraph.extractor.validation.JsonDocumentSchemaValidator;

import java.util.ArrayList;
import java.util.List;

@Service
public class QaModelExtractionService {

    private final JsonDocumentSchemaValidator inputValidator;
    private final JsonDocumentSchemaValidator outputValidator;
    private final StoryInputToQaModelMapper mapper;

    public QaModelExtractionService(
            @Qualifier("storyInputSchemaValidator")
            JsonDocumentSchemaValidator inputValidator,
            @Qualifier("generatedQaModelSchemaValidator")
            JsonDocumentSchemaValidator outputValidator,
            StoryInputToQaModelMapper mapper
    ) {
        this.inputValidator = inputValidator;
        this.outputValidator = outputValidator;
        this.mapper = mapper;
    }

    public QaModelExtractionResponse extract(JsonNode input) {
        List<ExtractionIssue> inputIssues = inputValidator.validate(input);

        if (hasErrors(inputIssues)) {
            return response(false, null, inputIssues);
        }

        StoryInputToQaModelMapper.MappingResult mapping = mapper.map(input);

        List<ExtractionIssue> issues = new ArrayList<>(mapping.issues());
        issues.addAll(outputValidator.validate(mapping.qaModel()));

        boolean extracted = !hasErrors(issues);

        return response(
                extracted,
                extracted ? mapping.qaModel() : null,
                issues
        );
    }

    private QaModelExtractionResponse response(
            boolean extracted,
            JsonNode qaModel,
            List<ExtractionIssue> issues
    ) {
        int errors = (int) issues.stream()
                .filter(issue -> issue.severity() == ExtractionSeverity.ERROR)
                .count();

        int warnings = (int) issues.stream()
                .filter(issue -> issue.severity() == ExtractionSeverity.WARNING)
                .count();

        return new QaModelExtractionResponse(
                extracted,
                "0.1",
                qaModel,
                new ExtractionSummary(errors, warnings, errors + warnings),
                List.copyOf(issues)
        );
    }

    private boolean hasErrors(List<ExtractionIssue> issues) {
        return issues.stream()
                .anyMatch(issue -> issue.severity() == ExtractionSeverity.ERROR);
    }
}
