package ru.kuznetsov.qaip.coverage.analyzer;
import com.fasterxml.jackson.databind.JsonNode; import org.springframework.stereotype.Component; import ru.kuznetsov.qaip.coverage.model.*; import java.util.*;
@Component public class RuleCoverageAnalyzer implements CoverageAnalyzer {
 public static final String METRIC_CODE="RULE_SCENARIO_COVERAGE";
 public CoverageAnalysisResult analyze(JsonNode qaModel){
  Map<String,NodeInfo> rules=indexRules(qaModel.path("nodes")); Set<String> covered=findCovered(qaModel.path("nodes"),qaModel.path("relationships")); List<CoverageProblem> problems=new ArrayList<>();
  for(NodeInfo rule:rules.values()) if(!covered.contains(rule.id())) problems.add(new CoverageProblem(CoverageProblemType.MISSING_SCENARIO,CoverageSeverity.WARNING,rule.id(),"BUSINESS_RULE",rule.name(),"Для бизнес-правила отсутствует покрывающий BDD-сценарий","BUSINESS_RULE покрыт, только если существует связь SCENARIO --COVERS--> BUSINESS_RULE.",rule.path()));
  int total=rules.size(); int cov=(int)rules.keySet().stream().filter(covered::contains).count(); int unc=total-cov;
  return new CoverageAnalysisResult(List.of(new CoverageMetric(METRIC_CODE,"Покрытие бизнес-правил сценариями",total,cov,unc,pct(cov,total))),List.copyOf(problems));
 }
 private Map<String,NodeInfo> indexRules(JsonNode nodes){Map<String,NodeInfo> r=new LinkedHashMap<>(); for(int i=0;i<nodes.size();i++){JsonNode n=nodes.get(i); if("BUSINESS_RULE".equals(text(n,"type"))) r.put(text(n,"id"),new NodeInfo(text(n,"id"),text(n,"name"),"/nodes/"+i));} return r;}
 private Set<String> findCovered(JsonNode nodes,JsonNode rels){Map<String,String> types=new HashMap<>(); for(JsonNode n:nodes) types.put(text(n,"id"),text(n,"type")); Set<String> r=new HashSet<>(); for(JsonNode x:rels) if("COVERS".equals(text(x,"type"))&&"SCENARIO".equals(types.get(text(x,"from")))&&"BUSINESS_RULE".equals(types.get(text(x,"to")))) r.add(text(x,"to")); return r;}
 private double pct(int c,int t){return t==0?100.0:Math.round(c*10000.0/t)/100.0;} private String text(JsonNode n,String f){JsonNode v=n.get(f); return v==null||v.isNull()?null:v.asText();}
 private record NodeInfo(String id,String name,String path){}
}
