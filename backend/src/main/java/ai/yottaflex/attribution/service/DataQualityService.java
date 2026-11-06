package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.DataQualityIssue;
import ai.yottaflex.attribution.entity.GroundTruth;
import ai.yottaflex.attribution.repository.DataQualityIssueRepository;
import ai.yottaflex.attribution.repository.GroundTruthRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class DataQualityService {
    private final DataQualityIssueRepository issues;
    private final GroundTruthRepository truth;
    private final JdbcTemplate jdbc;

    public DataQualityService(DataQualityIssueRepository issues,
            GroundTruthRepository truth,JdbcTemplate jdbc) {
        this.issues=issues;
        this.truth=truth;
        this.jdbc=jdbc;
    }

    @Transactional
    public Map<String,Object> inspect(String runId) {
        Instant detectedAt=Instant.now();
        List<DataQualityIssue> found=new ArrayList<>();
        List<GroundTruth> labels=new ArrayList<>();

        List<Map<String,Object>> duplicateRows=jdbc.queryForList("""
            select external_revenue_key,count(*) row_count
            from orders o
            join opportunities opp on opp.id=o.opportunity_id
            join leads l on l.id=opp.lead_id
            join contacts c on c.id=l.contact_id
            where (? is null or c.dataset_run_id=?)
            group by external_revenue_key
            having count(*)>1
            """,runId,runId);
        for(Map<String,Object> row:duplicateRows) {
            String revenueKey=String.valueOf(row.get("external_revenue_key"));
            add(found,labels,"DUPLICATE_REVENUE","ORDER",revenueKey,"ERROR",
                row.get("row_count")+" order records share this revenue key. Count it once.",
                "COUNT_UNIQUE_REVENUE_KEY_ONCE",detectedAt,runId);
        }

        List<Map<String,Object>> lateRows=jdbc.queryForList("""
            select distinct t.id touchpoint_id
            from touchpoints t
            join leads l on l.contact_id=t.contact_id
            join opportunities o on o.lead_id=l.id
            join contacts c on c.id=t.contact_id
            where t.event_at>o.closed_at
              and (? is null or c.dataset_run_id=?)
            """,runId,runId);
        for(Map<String,Object> row:lateRows) {
            add(found,labels,"POST_CONVERSION_TOUCH","TOUCHPOINT",
                String.valueOf(row.get("touchpoint_id")),"WARNING",
                "Touchpoint occurs after a linked opportunity closed; exclude it.",
                "EXCLUDE_POST_CONVERSION",detectedAt,runId);
        }

        List<Map<String,Object>> fxRows=jdbc.queryForList("""
            select cost.id cost_id
            from cost_records cost
            join campaigns campaign on campaign.id=cost.campaign_id
            where cost.currency<>campaign.currency
              and (? is null or campaign.dataset_run_id=?)
            """,runId,runId);
        for(Map<String,Object> row:fxRows) {
            add(found,labels,"CURRENCY_MISMATCH","COST_RECORD",
                String.valueOf(row.get("cost_id")),"ERROR",
                "Cost currency differs from campaign currency; ROI requires reconciliation.",
                "QUARANTINE_UNTIL_FX",detectedAt,runId);
        }

        List<Map<String,Object>> missingRows=jdbc.queryForList("""
            select campaign.id campaign_id
            from campaigns campaign
            where not exists(select 1 from cost_records cost where cost.campaign_id=campaign.id)
              and (? is null or campaign.dataset_run_id=?)
            """,runId,runId);
        for(Map<String,Object> row:missingRows) {
            add(found,labels,"MISSING_CAMPAIGN_COST","CAMPAIGN",
                String.valueOf(row.get("campaign_id")),"WARNING",
                "Campaign has no cost records. ROI is unavailable, not zero.",
                "MARK_ROI_UNAVAILABLE",detectedAt,runId);
        }

        List<Map<String,Object>> identityRows=jdbc.queryForList("""
            select id session_id
            from web_sessions
            where contact_id is not null and identity_link_verified=false
              and (? is null or dataset_run_id=?)
            """,runId,runId);
        for(Map<String,Object> row:identityRows) {
            add(found,labels,"UNVERIFIED_IDENTITY_LINK","WEB_SESSION",
                String.valueOf(row.get("session_id")),"ERROR",
                "Session links a contact without verified identity evidence.",
                "REJECT_IDENTITY_LINK",detectedAt,runId);
        }

        // Findings are append-only; issue IDs are unique per inspection.
        issues.saveAll(found);
        truth.saveAll(labels);
        Map<String,Long> byType=new LinkedHashMap<>();
        found.forEach(issue->byType.merge(issue.getIssueType(),1L,Long::sum));

        return Map.of("runId",runId==null?"ALL":runId,
            "issueCount",found.size(),"groundTruthCount",labels.size(),
            "byType",byType,
            "issues",found.stream().map(issue->Map.of(
                "id",issue.getId(),"type",issue.getIssueType(),
                "severity",issue.getSeverity(),"description",issue.getDescription()
            )).toList());
    }

    private void add(List<DataQualityIssue> found,List<GroundTruth> labels,
            String type,String entity,String entityId,String severity,
            String description,String decision,Instant detectedAt,String runId) {
        String issueId=UUID.randomUUID().toString();
        found.add(new DataQualityIssue(issueId,type,entity,entityId,severity,
            description,decision,detectedAt,runId));
        labels.add(new GroundTruth("GT-"+issueId,type,entity,entityId,decision,"TEST",runId));
    }
}
