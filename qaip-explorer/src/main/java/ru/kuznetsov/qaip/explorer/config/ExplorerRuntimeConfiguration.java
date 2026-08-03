package ru.kuznetsov.qaip.explorer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.DefaultRepositoryAnalysisService;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisService;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryService;
import ru.kuznetsov.qaip.explorer.application.DefaultAnalyzeRepositoryService;
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
    AnalyzeRepositoryService analyzeRepositoryService(
            RepositoryAnalysisService repositoryAnalysisService,
            QaipRuntime qaipRuntime
    ) {
        return new DefaultAnalyzeRepositoryService(
                repositoryAnalysisService, qaipRuntime.importProjectUseCase());
    }
}
