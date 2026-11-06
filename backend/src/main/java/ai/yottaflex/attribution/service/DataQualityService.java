package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.entity.*;
import ai.yottaflex.attribution.repository.*;
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
    public DataQualityService(DataQualityIssueRepository issues,GroundTruthRepository truth,JdbcTemplate jdbc){this.issues=issues;this.truth=truth;this.jdbc=jdbc;}
    @Transactional
    public Map<String,Object> inspect(){
        issues.deleteAllInBatch();truth.deleteAllInBatch();
        List<DataQualityIssue> found=new ArrayList<>();List<GroundTruth> labels=new ArrayList<>();
        long duplicateCount=jdbc.queryForObject("select count(*) from (select external_revenue_key from orders group by external_revenue_key having count(*)>1) x",Long.class);
        List<Map<String,Object>> duplicateRows=jdbc.queryForList("select external_revenue_key,count(*) row_count from orders group by external_revenue_key having count(*)>1");
        if(duplicateCount>0)for(Map<String,Object> row:duplicateRows)add(found,labels,"DUPLICATE_REVENUE","ORDER",String.valueOf(row.get("external_revenue_key")),"ERROR",row.get("row_count")+" order records share this revenue key. Aggregate once.","COUNT_UNIQUE_REVENUE_KEY_ONCE");
        List<Map<String,Object>> lateRows=jdbc.queryForList("""
            select distinct t.id touchpoint_id from touchpoints t
            join leads l on l.contact_id=t.contact_id join opportunities o on o.lead_id=l.id
            where t.event_at>o.closed_at
            """);
        for(Map<String,Object> row:lateRows)add(found,labels,"POST_CONVERSION_TOUCH","TOUCHPOINT",String.valueOf(row.get("touchpoint_id")),"WARNING","Touchpoint occurs after a linked opportunity closed; exclude it from attribution.","EXCLUDE_POST_CONVERSION");
        List<Map<String,Object>> fxRows=jdbc.queryForList("select c.id cost_id from cost_records c join campaigns p on p.id=c.campaign_id where c.currency<>p.currency");
        for(Map<String,Object> row:fxRows)add(found,labels,"CURRENCY_MISMATCH","COST_RECORD",String.valueOf(row.get("cost_id")),"ERROR","Cost currency differs from campaign currency. Do not calculate mixed-currency ROI.","QUARANTINE_UNTIL_FX");
        List<Map<String,Object>> missingRows=jdbc.queryForList("select p.id campaign_id from campaigns p where not exists(select 1 from cost_records c where c.campaign_id=p.id)");
        for(Map<String,Object> row:missingRows)add(found,labels,"MISSING_CAMPAIGN_COST","CAMPAIGN",String.valueOf(row.get("campaign_id")),"WARNING","Campaign has no cost records. ROI is unavailable, not zero.","MARK_ROI_UNAVAILABLE");
        List<Map<String,Object>> identityRows=jdbc.queryForList("select id session_id from web_sessions where contact_id is not null and identity_link_verified=false");
        for(Map<String,Object> row:identityRows)add(found,labels,"UNVERIFIED_IDENTITY_LINK","WEB_SESSION",String.valueOf(row.get("session_id")),"ERROR","Session links a contact without identity evidence.","REJECT_IDENTITY_LINK");
        issues.saveAll(found);truth.saveAll(labels);
        Map<String,Long> byType=new LinkedHashMap<>();
        found.forEach(i->byType.merge(i.getIssueType(),1L,Long::sum));
        return Map.of("issueCount",found.size(),"groundTruthCount",labels.size(),"byType",byType,
            "issues",found.stream().map(i->Map.of("id",i.getId(),"type",i.getIssueType(),"severity",i.getSeverity(),"description",i.getDescription())).toList());
    }
    private void add(List<DataQualityIssue> found,List<GroundTruth> labels,String type,String entity,String entityId,String severity,String description,String decision){
        String key=type+"-"+entityId;
        found.add(new DataQualityIssue(key,type,entity,entityId,severity,description,decision,Instant.now()));
        labels.add(new GroundTruth("GT-"+key,type,entity,entityId,decision,"TEST"));
    }
}
