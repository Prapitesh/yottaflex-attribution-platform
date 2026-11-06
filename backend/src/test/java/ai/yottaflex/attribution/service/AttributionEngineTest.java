package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.*;
import ai.yottaflex.attribution.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributionEngineTest {
    @Mock private OpportunityRepository opportunities;
    @Mock private TouchpointRepository touchpoints;
    @Mock private AttributionResultRepository results;
    @Mock private JdbcTemplate jdbc;
    private AttributionEngine engine;

    @BeforeEach
    void setUp() {
        engine=new AttributionEngine(opportunities,touchpoints,results,jdbc);
    }

    @Test
    void firstLastAndLinearProduceCorrectCredits() {
        Fixture fixture=fixture();
        when(opportunities.findAll()).thenReturn(List.of(fixture.opportunity));
        when(touchpoints.findAll()).thenReturn(List.of(fixture.early,fixture.middle,fixture.late));

        Map<String,Object> outcome=engine.calculate(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttributionResult>> captor=ArgumentCaptor.forClass(List.class);
        verify(results).saveAll(captor.capture());
        List<AttributionResult> saved=captor.getValue();

        assertEquals(5,outcome.get("resultRowsCreated"));
        assertEquals(5,saved.size());
        List<AttributionResult> first=saved.stream().filter(r->"FIRST_TOUCH".equals(r.getModelName())).toList();
        List<AttributionResult> last=saved.stream().filter(r->"LAST_TOUCH".equals(r.getModelName())).toList();
        List<AttributionResult> linear=saved.stream().filter(r->"LINEAR".equals(r.getModelName())).toList();

        assertEquals(fixture.early.getId(),first.getFirst().getTouchpoint().getId());
        assertEquals(fixture.late.getId(),last.getFirst().getTouchpoint().getId());
        assertEquals(3,linear.size());
        BigDecimal total=linear.stream().map(AttributionResult::getCredit)
            .reduce(BigDecimal.ZERO,BigDecimal::add);
        assertEquals(0,BigDecimal.ONE.compareTo(total));
        assertTrue(linear.stream().allMatch(r->r.getCredit().compareTo(new BigDecimal("0.333333"))>=0));
    }

    @Test
    void postConversionAndOutsideLookbackTouchesAreExcluded() {
        Fixture fixture=fixture();
        Touchpoint tooOld=touch("TOO-OLD",fixture.contact,Instant.parse("2025-01-01T00:00:00Z"));
        Touchpoint after=touch("AFTER",fixture.contact,Instant.parse("2025-08-01T00:00:00Z"));
        when(opportunities.findAll()).thenReturn(List.of(fixture.opportunity));
        when(touchpoints.findAll()).thenReturn(List.of(tooOld,fixture.early,fixture.middle,fixture.late,after));

        engine.calculate(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttributionResult>> captor=ArgumentCaptor.forClass(List.class);
        verify(results).saveAll(captor.capture());
        List<AttributionResult> saved=captor.getValue();
        assertTrue(saved.stream().noneMatch(r->r.getTouchpoint().getId().equals("TOO-OLD")));
        assertTrue(saved.stream().noneMatch(r->r.getTouchpoint().getId().equals("AFTER")));
    }

    @Test
    void opportunityWithoutEligibleTouchIsReportedAndNotAttributed() {
        Fixture fixture=fixture();
        when(opportunities.findAll()).thenReturn(List.of(fixture.opportunity));
        when(touchpoints.findAll()).thenReturn(List.of());

        Map<String,Object> outcome=engine.calculate(null);

        assertEquals(1,outcome.get("opportunitiesWithoutEligibleTouches"));
        assertEquals(0,outcome.get("resultRowsCreated"));
        verify(results).saveAll(List.of());
    }

    private Fixture fixture() {
        Channel channel=new Channel("CH-1","Search","PAID");
        Campaign campaign=new Campaign("CMP-1","Campaign",channel,new BigDecimal("1000.00"),"INR",
            java.time.LocalDate.parse("2025-01-01"),java.time.LocalDate.parse("2026-12-31"),"COMPLETED");
        Contact contact=new Contact("CON-1","SMB",Instant.parse("2025-01-01T00:00:00Z"));
        WebSession session=new WebSession("SES-1",contact,null,Instant.parse("2025-06-01T00:00:00Z"),"/",true);
        Lead lead=new Lead("LEAD-1",contact,session,Instant.parse("2025-06-01T00:00:00Z"),"CONVERTED");
        Opportunity opportunity=new Opportunity("OPP-1",lead,Instant.parse("2025-06-01T00:00:00Z"),
            Instant.parse("2025-07-01T00:00:00Z"),"WON",new BigDecimal("1200.00"),"INR");
        Touchpoint early=touch("EARLY",contact,Instant.parse("2025-06-01T00:00:00Z"));
        Touchpoint middle=touch("MIDDLE",contact,Instant.parse("2025-06-15T00:00:00Z"));
        Touchpoint late=touch("LATE",contact,Instant.parse("2025-06-30T00:00:00Z"));
        return new Fixture(contact,opportunity,early,middle,late);
    }

    private Touchpoint touch(String id,Contact contact,Instant time) {
        Channel channel=new Channel("CH-"+id,"Channel "+id,"PAID");
        Campaign campaign=new Campaign("CMP-"+id,"Campaign "+id,channel,new BigDecimal("100.00"),"INR",
            java.time.LocalDate.parse("2025-01-01"),java.time.LocalDate.parse("2026-12-31"),"COMPLETED");
        return new Touchpoint(id,contact,campaign,null,channel,time,"CLICK","ONLINE");
    }

    private record Fixture(Contact contact,Opportunity opportunity,Touchpoint early,Touchpoint middle,Touchpoint late) {}
}
