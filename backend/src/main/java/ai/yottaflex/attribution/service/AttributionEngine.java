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
    private static final BigDecimal ZERO=BigDecimal.ZERO;
    private static final long LOOKBACK_DAYS=180;

    public AttributionEngine(OpportunityRepository opportunities,TouchpointRepository touchpoints,AttributionResultRepository results){
        this.opportunities=opportunities;this.touchpoints=touchpoints;this.results=results;
    }

    @Transactional
    public Map<String,Object> calculate(){
        List<Opportunity> opps=opportunities.findAll();
        List<Touchpoint> allTouches=touchpoints.findAll();
        Map<String,List<Touchpoint>> journey=new HashMap<>();
        for(Touchpoint t:allTouches) journey.computeIfAbsent(t.getContact().getId(),k->new ArrayList<>()).add(t);
        List<AttributionResult> generated=new ArrayList<>();
        Instant calculatedAt=Instant.now();
        for(AttributionModel model:AttributionModel.values()){
            for(Opportunity opp:opps){
                Instant conversion=opp.getClosedAt();
                Instant min=conversion.minusSeconds(LOOKBACK_DAYS*86400);
                List<Touchpoint> eligible=journey.getOrDefault(opp.getLead().getContact().getId(),List.of()).stream()
                        .filter(t->!t.getEventAt().isAfter(conversion)&&!t.getEventAt().isBefore(min))
                        .sorted(Comparator.comparing(Touchpoint::getEventAt)).toList();
                if(eligible.isEmpty()) continue;
                List<Touchpoint> credited=switch(model){
                    case FIRST_TOUCH -> List.of(eligible.getFirst());
                    case LAST_TOUCH -> List.of(eligible.getLast());
                    case LINEAR -> eligible;
                };
                BigDecimal equalCredit=BigDecimal.ONE.divide(BigDecimal.valueOf(credited.size()),6,RoundingMode.HALF_UP);
                BigDecimal assigned=ZERO;
                for(int i=0;i<credited.size();i++){
                    BigDecimal share=(i==credited.size()-1)?BigDecimal.ONE.subtract(assigned):equalCredit;
                    assigned=assigned.add(share);
                    BigDecimal amount=opp.getExpectedRevenue().multiply(share).setScale(2,RoundingMode.HALF_UP);
                    generated.add(new AttributionResult(UUID.randomUUID().toString(),opp,credited.get(i),model.name(),share,amount,calculatedAt));
                }
            }
        }
        results.deleteAllInBatch();
        results.saveAll(generated);
        return Map.of("opportunitiesEvaluated",opps.size(),"resultRows",generated.size(),
                "models",List.of("FIRST_TOUCH","LAST_TOUCH","LINEAR"),"lookbackDays",LOOKBACK_DAYS);
    }
}
