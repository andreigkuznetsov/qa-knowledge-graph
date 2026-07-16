package ru.kuznetsov.qaip.coverage.traceability.analysis;

import org.springframework.stereotype.Component;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityChain;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityChainBuildResult;
import ru.kuznetsov.qaip.coverage.traceability.model.TraceabilityNodeRef;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TraceabilityCoverageAnalyzer {

    public TraceabilityCoverageAnalysis analyze(TraceabilityChainBuildResult result) {
        List<AnalyzedTraceabilityChain> chains = new ArrayList<>();
        List<TraceabilityProblem> problems = new ArrayList<>();
        Map<TraceabilityStatus,Integer> counts = new EnumMap<>(TraceabilityStatus.class);
        int depthSum = 0;
        int maxDepth = 0;

        for (TraceabilityChain chain : result.chains()) {
            TraceabilityStatus status = status(chain.depth());
            counts.merge(status, 1, Integer::sum);
            depthSum += chain.depth();
            maxDepth = Math.max(maxDepth, chain.depth());
            chains.add(new AnalyzedTraceabilityChain(
                    chain,
                    status,
                    status == TraceabilityStatus.FULLY_TRACEABLE
                            ? null
                            : chain.lastNode().type()));
            if (status != TraceabilityStatus.FULLY_TRACEABLE) {
                problems.add(problem(chain, status));
            }
        }

        int total = result.totalChains();
        int full = count(counts, TraceabilityStatus.FULLY_TRACEABLE);
        TraceabilityCoverageSummary summary = new TraceabilityCoverageSummary(
                total, full, total-full, percentage(full,total),
                total == 0 ? 0.0 : Math.round(depthSum*100.0/total)/100.0,
                maxDepth);
        TraceabilityBreakdown breakdown = new TraceabilityBreakdown(
                full,
                count(counts, TraceabilityStatus.BROKEN_AT_OPERATION),
                count(counts, TraceabilityStatus.BROKEN_AT_RULE),
                count(counts, TraceabilityStatus.BROKEN_AT_SCENARIO),
                count(counts, TraceabilityStatus.BROKEN_AT_TEST_IMPLEMENTATION),
                count(counts, TraceabilityStatus.BROKEN_AT_CHECK));
        return new TraceabilityCoverageAnalysis(
                summary, breakdown, List.copyOf(chains), List.copyOf(problems));
    }

    private TraceabilityStatus status(int depth) {
        return switch (depth) {
            case 1 -> TraceabilityStatus.BROKEN_AT_OPERATION;
            case 2 -> TraceabilityStatus.BROKEN_AT_RULE;
            case 3 -> TraceabilityStatus.BROKEN_AT_SCENARIO;
            case 4 -> TraceabilityStatus.BROKEN_AT_TEST_IMPLEMENTATION;
            case 5 -> TraceabilityStatus.BROKEN_AT_CHECK;
            case 6 -> TraceabilityStatus.FULLY_TRACEABLE;
            default -> throw new IllegalArgumentException("Недопустимая глубина: " + depth);
        };
    }

    private TraceabilityProblem problem(TraceabilityChain chain, TraceabilityStatus status) {
        TraceabilityNodeRef last = chain.lastNode();
        return new TraceabilityProblem(
                chain.id(), chain.userStory().id(), status,
                last.id(), last.type(), message(status), recommendation(status,last),
                chain.nodeIds());
    }

    private String message(TraceabilityStatus status) {
        return switch (status) {
            case BROKEN_AT_OPERATION -> "Для пользовательской истории отсутствует бизнес-операция";
            case BROKEN_AT_RULE -> "Для бизнес-операции отсутствует бизнес-правило";
            case BROKEN_AT_SCENARIO -> "Для бизнес-правила отсутствует BDD-сценарий";
            case BROKEN_AT_TEST_IMPLEMENTATION -> "Для BDD-сценария отсутствует тестовая реализация";
            case BROKEN_AT_CHECK -> "Для тестовой реализации отсутствует атомарная проверка";
            case FULLY_TRACEABLE -> "Цепочка полностью трассируема";
        };
    }

    private String recommendation(TraceabilityStatus status, TraceabilityNodeRef last) {
        return switch (status) {
            case BROKEN_AT_OPERATION -> "Добавьте BUSINESS_OPERATION и связь " + last.id() + " --DESCRIBES--> BUSINESS_OPERATION.";
            case BROKEN_AT_RULE -> "Добавьте BUSINESS_RULE и связь " + last.id() + " --GOVERNED_BY--> BUSINESS_RULE.";
            case BROKEN_AT_SCENARIO -> "Добавьте SCENARIO и связь SCENARIO --COVERS--> " + last.id() + ".";
            case BROKEN_AT_TEST_IMPLEMENTATION -> "Добавьте TEST_IMPLEMENTATION и связь TEST_IMPLEMENTATION --VALIDATES--> " + last.id() + ".";
            case BROKEN_AT_CHECK -> "Добавьте CHECK и связь " + last.id() + " --HAS_CHECK--> CHECK.";
            case FULLY_TRACEABLE -> "Дополнительные действия не требуются.";
        };
    }

    private int count(Map<TraceabilityStatus,Integer> counts, TraceabilityStatus status) {
        return counts.getOrDefault(status,0);
    }

    private double percentage(int covered, int total) {
        return total == 0 ? 100.0 : Math.round(covered*10000.0/total)/100.0;
    }
}
