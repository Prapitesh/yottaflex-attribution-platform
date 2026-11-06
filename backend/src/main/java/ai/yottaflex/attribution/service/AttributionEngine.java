package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.*;
import ai.yottaflex.attribution.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.Instant;
import java.util.*;

@Service
public class AttributionEngine {
    private final OpportunityRepository opportunities;
    private final TouchpointRepository touchpoints;
    private final AttributionResultRepository results;
    private static final long LOOKBACK_DAYS=180;
    public AttributionEngine(OpportunityRepository opportunities,TouchpointRepository touchpoints,AttributionResultRepository results){this.opportunities=opportunities;this.touchpoints=touchpoints;this.results=results;}
    @Transactional
    public Map<String,Object> calculate(){
        List<Opportunity> opps=opportunities.findAll();List<Touchpoint> all=touchpoints.findAll();
        Map<String,List<Touchpoint>> journeys=new HashMap<>();
        for(Touchpoint t:all)journeys.computeIfAbsent(t.getContact().getId(),k->new ArrayList<>()).add(t);
        List<AttributionResult> generated=new ArrayList<>();Instant now=Instant.now();int withoutTouch=0;
        for(Opportunity opp:opps)for(AttributionModel model:AttributionModel.values()){
            Instant close=opp.getClosedAt(),start=close.minusSeconds(LOOKBACK_DAYS*86400L);
            List<Touchpoint> eligible=journeys.getOrDefault(opp.getLead().getContact().getId(),List.of()).stream()
                .filter(t->!t.getEventAt().isAfter(close)&&!t.getEventAt().isBefore(start))
                .sorted(Comparator.comparing(Touchpoint::getEventAt)).toList();
            if(eligible.isEmpty()){withoutTouch++;continue;}
            List<Touchpoint> selected=switch(model){case FIRST_TOUCH->List.of(eligible.getFirst());case LAST_TOUCH->List.of(eligible.getLast());case LINEAR->eligible;};
            BigDecimal each=BigDecimal.ONE.divide(BigDecimal.valueOf(selected.size()),6,RoundingMode.DOWN),assigned=BigDecimal.ZERO;
            for(int i=0;i<selected.size();i++){
                BigDecimal credit=i==selected.size()-1?BigDecimal.ONE.subtract(assigned):each;assigned=assigned.add(credit);
                BigDecimal attributed=opp.getExpectedRevenue().multiply(credit).setScale(2,RoundingMode.HALF_UP);
                generated.add(new AttributionResult(UUID.randomUUID().toString(),opp,selected.get(i),model.name(),credit,attributed,now));
            }
        }
        results.deleteAllInBatch();results.saveAll(generated);
        return Map.of("opportunitiesEvaluated",opps.size(),"resultRows",generated.size(),"withoutEligibleTouchModelPairs",withoutTouch,"models",List.of("FIRST_TOUCH","LAST_TOUCH","LINEAR"),"lookbackDays",LOOKBACK_DAYS);
    }
}
