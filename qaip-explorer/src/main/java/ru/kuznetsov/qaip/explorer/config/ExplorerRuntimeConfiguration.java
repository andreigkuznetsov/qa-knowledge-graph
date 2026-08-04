package ru.kuznetsov.qaip.explorer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.DefaultRepositoryAnalysisService;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisService;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryService;
import ru.kuznetsov.qaip.explorer.application.DefaultOperationListService;
import ru.kuznetsov.qaip.explorer.application.DefaultOperationDetailsService;
import ru.kuznetsov.qaip.explorer.application.DefaultAnalyzeRepositoryService;
import ru.kuznetsov.qaip.explorer.application.InMemoryRepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.application.DefaultRepositorySummaryService;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.application.OperationListGateway;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsGateway;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsService;
import ru.kuznetsov.qaip.explorer.application.OperationListService;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryGateway;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryService;
import ru.kuznetsov.qaip.explorer.adapter.runtime.RuntimeRepositorySummaryAdapter;
import ru.kuznetsov.qaip.explorer.adapter.runtime.RuntimeOperationListAdapter;
import ru.kuznetsov.qaip.explorer.adapter.runtime.RuntimeOperationDetailsAdapter;
import ru.kuznetsov.qaip.runtime.QaipRuntime;
import ru.kuznetsov.qaip.runtime.QaipRuntimeFactory;

@Configuration
public class ExplorerRuntimeConfiguration {

    @Bean
    RepositoryAnalysisService repositoryAnalysisService() {
        return new DefaultRepositoryAnalysisService();
    }

    @Bean
    QaipRuntime qaipRuntime() {
        return QaipRuntimeFactory.inMemory();
    }

    @Bean
    RepositoryAnalysisCatalog repositoryAnalysisCatalog() {
        return new InMemoryRepositoryAnalysisCatalog();
    }

    @Bean
    AnalyzeRepositoryService analyzeRepositoryService(
            RepositoryAnalysisService repositoryAnalysisService,
            QaipRuntime qaipRuntime,
            RepositoryAnalysisCatalog catalog
    ) {
        return new DefaultAnalyzeRepositoryService(
                repositoryAnalysisService, qaipRuntime.importProjectUseCase(), catalog);
    }

    @Bean
    RepositorySummaryGateway repositorySummaryGateway(
            QaipRuntime qaipRuntime,
            RepositoryAnalysisCatalog catalog
    ) {
        return new RuntimeRepositorySummaryAdapter(qaipRuntime, catalog);
    }

    @Bean
    RepositorySummaryService repositorySummaryService(RepositorySummaryGateway gateway) {
        return new DefaultRepositorySummaryService(gateway);
    }

    @Bean
    OperationListGateway operationListGateway(QaipRuntime qaipRuntime, RepositoryAnalysisCatalog catalog) {
        return new RuntimeOperationListAdapter(qaipRuntime, catalog);
    }

    @Bean
    OperationListService operationListService(OperationListGateway gateway) {
        return new DefaultOperationListService(gateway);
    }

    @Bean
    OperationDetailsGateway operationDetailsGateway(QaipRuntime qaipRuntime, RepositoryAnalysisCatalog catalog) {
        return new RuntimeOperationDetailsAdapter(qaipRuntime, catalog);
    }

    @Bean
    OperationDetailsService operationDetailsService(OperationDetailsGateway gateway) {
        return new DefaultOperationDetailsService(gateway);
    }
}
