package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.AttributionResult;
import ai.yottaflex.attribution.entity.Opportunity;
import ai.yottaflex.attribution.entity.Touchpoint;
import ai.yottaflex.attribution.repository.AttributionResultRepository;
import ai.yottaflex.attribution.repository.OpportunityRepository;
import ai.yottaflex.attribution.repository.TouchpointRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class AttributionEngine {
    private static final int LOOKBACK_DAYS=180;
    private static final int CREDIT_SCALE=6;
    private final OpportunityRepository opportunities;
    private final TouchpointRepository touchpoints;
    private final AttributionResultRepository results;
    private final JdbcTemplate jdbc;

    public AttributionEngine(OpportunityRepository opportunities,
            TouchpointRepository touchpoints,AttributionResultRepository results,
            JdbcTemplate jdbc) {
        this.opportunities=opportunities;
        this.touchpoints=touchpoints;
        this.results=results;
        this.jdbc=jdbc;
    }

    @Transactional
    public Map<String,Object> calculate(String runId) {
        List<Opportunity> opportunityRows;
        List<Touchpoint> touchRows;
        if(runId==null) {
            opportunityRows=opportunities.findAll();
            touchRows=touchpoints.findAll();
        } else {
            opportunityRows=opportunities.findByLeadContactDatasetRunId(runId);
            touchRows=touchpoints.findByContactDatasetRunId(runId);
        }

        Map<String,List<Touchpoint>> journeys=new HashMap<>();
        for(Touchpoint touch:touchRows) {
            journeys.computeIfAbsent(touch.getContact().getId(),key->new ArrayList<>()).add(touch);
        }
        journeys.values().forEach(rows->rows.sort(
            Comparator.comparing(Touchpoint::getEventAt).thenComparing(Touchpoint::getId)));

        List<AttributionResult> generated=new ArrayList<>();
        Instant calculatedAt=Instant.now();
        int opportunitiesWithoutEligibleTouches=0;

        for(Opportunity opportunity:opportunityRows) {
            Instant conversionAt=opportunity.getClosedAt();
            Instant lookbackStart=conversionAt.minusSeconds(LOOKBACK_DAYS*86400L);
            List<Touchpoint> journey=journeys.getOrDefault(
                opportunity.getLead().getContact().getId(),List.of());

            List<Touchpoint> eligible=journey.stream()
                .filter(touch->!touch.getEventAt().isAfter(conversionAt))
                .filter(touch->!touch.getEventAt().isBefore(lookbackStart))
                .toList();

            if(eligible.isEmpty()) {
                opportunitiesWithoutEligibleTouches++;
                continue;
            }

            for(AttributionModel model:AttributionModel.values()) {
                List<Touchpoint> credited=switch(model) {
                    case FIRST_TOUCH -> List.of(eligible.getFirst());
                    case LAST_TOUCH -> List.of(eligible.getLast());
                    case LINEAR -> eligible;
                };

                BigDecimal equalShare=BigDecimal.ONE.divide(
                    BigDecimal.valueOf(credited.size()),CREDIT_SCALE,RoundingMode.DOWN);
                BigDecimal assigned=BigDecimal.ZERO;

                for(int index=0;index<credited.size();index++) {
                    BigDecimal credit=(index==credited.size()-1)
                        ?BigDecimal.ONE.subtract(assigned):equalShare;
                    assigned=assigned.add(credit);

                    BigDecimal attributedRevenue=opportunity.getExpectedRevenue()
                        .multiply(credit).setScale(2,RoundingMode.HALF_UP);

                    generated.add(new AttributionResult(UUID.randomUUID().toString(),
                        opportunity,credited.get(index),model.name(),credit,
                        attributedRevenue,calculatedAt));
                }
            }
        }

        // Calculation rows are append-only and tied to the run via the opportunity.
        results.saveAll(generated);
        if(runId!=null) {
            jdbc.update("update dataset_runs set status='VALIDATED' where id=?",runId);
        }
        return Map.of(
            "runId",runId==null?"ALL":runId,
            "opportunitiesEvaluated",opportunityRows.size(),
            "resultRowsCreated",generated.size(),
            "opportunitiesWithoutEligibleTouches",opportunitiesWithoutEligibleTouches,
            "models",List.of("FIRST_TOUCH","LAST_TOUCH","LINEAR"),
            "lookbackDays",LOOKBACK_DAYS,
            "calculatedAt",calculatedAt.toString()
        );
    }
}
