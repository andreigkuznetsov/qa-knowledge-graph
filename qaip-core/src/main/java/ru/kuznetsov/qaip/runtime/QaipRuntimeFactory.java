package ru.kuznetsov.qaip.runtime;

import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.application.importproject.DefaultImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.persistence.DefaultPersistProject;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.application.query.eventpath.DefaultEventPathQuery;
import ru.kuznetsov.qaip.core.application.query.nodedetails.DefaultNodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsMapper;
import ru.kuznetsov.qaip.core.application.query.operationdetails.DefaultOperationDetailsQuery;
import ru.kuznetsov.qaip.core.application.query.operationdetails.ConventionalImplementationPathResolver;
import ru.kuznetsov.qaip.core.application.query.operationlist.DefaultOperationListQuery;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.application.query.operationoverview.DefaultOperationOverviewQuery;
import ru.kuznetsov.qaip.core.application.query.operationtests.DefaultOperationTestsQuery;
import ru.kuznetsov.qaip.core.application.query.projectsummary.DefaultProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryMapper;
import ru.kuznetsov.qaip.core.application.query.relationship.DefaultRelationshipsUseCase;
import ru.kuznetsov.qaip.core.application.query.relationship.ProjectRelationshipLookup;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipDetailsMapper;
import ru.kuznetsov.qaip.core.application.query.trace.DefaultTraceUseCase;
import ru.kuznetsov.qaip.core.application.query.trace.TraceGraphBuilder;
import ru.kuznetsov.qaip.core.application.query.trace.TraceMapper;
import ru.kuznetsov.qaip.core.application.query.validation.DefaultValidationUseCase;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationReportMapper;
import ru.kuznetsov.qaip.core.application.validation.ValidationEngine;
import ru.kuznetsov.qaip.core.application.validation.rule.IsolatedNodeValidationRule;
import ru.kuznetsov.qaip.core.application.validation.rule.ScenarioWithoutTestValidationRule;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.util.List;

public final class QaipRuntimeFactory {
    private QaipRuntimeFactory() { }

    public static QaipRuntime inMemory() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        ProjectImporter importer = new DefaultProjectImporter(
                new JacksonProjectJsonParser(),
                new NetworkntProjectSchemaValidator(),
                new DefaultProjectBinder(),
                new DefaultProjectApplicationValidator());
        var importUseCase = new DefaultImportProjectUseCase(
                importer, new DefaultPersistProject(repository));

        ProjectNodeLookup nodeLookup = new ProjectNodeLookup();
        ValidationEngine validationEngine = new ValidationEngine(List.of(
                new IsolatedNodeValidationRule(), new ScenarioWithoutTestValidationRule()));
        OperationListProjector operationProjector = new OperationListProjector();
        ConventionalImplementationPathResolver implementationResolver =
                new ConventionalImplementationPathResolver();

        return new QaipRuntime(
                importUseCase,
                reader,
                new DefaultEventPathQuery(reader),
                new DefaultOperationDetailsQuery(reader, operationProjector, implementationResolver),
                new DefaultOperationListQuery(reader, operationProjector),
                new DefaultOperationOverviewQuery(reader, implementationResolver),
                new DefaultOperationTestsQuery(reader, operationProjector),
                new DefaultProjectSummaryUseCase(reader, new ProjectSummaryMapper()),
                new DefaultNodeDetailsUseCase(reader, nodeLookup, new NodeDetailsMapper()),
                new DefaultRelationshipsUseCase(
                        reader, nodeLookup, new ProjectRelationshipLookup(), new RelationshipDetailsMapper()),
                new DefaultTraceUseCase(reader, nodeLookup, new TraceGraphBuilder(), new TraceMapper()),
                new DefaultValidationUseCase(reader, validationEngine, new ValidationReportMapper()));
    }
}
