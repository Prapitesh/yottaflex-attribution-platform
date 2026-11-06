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
        long duplicateRevenue=jdbc.queryForObject("select count(*) from (select external_revenue_key from orders group by external_revenue_key having count(*)>1) d",Long.class);
        if(duplicateRevenue>0)add(found,labels,"DUPLICATE_REVENUE","ORDER","ALL","ERROR",duplicateRevenue+" duplicate revenue keys were detected. Count each external revenue key once.","EXCLUDE_DUPLICATE");
        long postConversion=jdbc.queryForObject("select count(*) from touchpoints t join leads l on l.contact_id=t.contact_id join opportunities o on o.lead_id=l.id where t.event_at>o.closed_at",Long.class);
        if(postConversion>0)add(found,labels,"POST_CONVERSION_TOUCH","TOUCHPOINT","MULTIPLE","WARNING",postConversion+" touchpoints occur after a related opportunity closed; exclude them from attribution.","EXCLUDE_POST_CONVERSION");
        long mismatch=jdbc.queryForObject("select count(*) from cost_records c join campaigns p on p.id=c.campaign_id where c.currency<>p.currency",Long.class);
        if(mismatch>0)add(found,labels,"CURRENCY_MISMATCH","COST_RECORD","MULTIPLE","ERROR",mismatch+" campaign cost rows use a different currency from their campaign. ROI is not comparable without an FX policy.","QUARANTINE_UNTIL_FX");
        long missing=jdbc.queryForObject("select count(*) from campaigns p where not exists(select 1 from cost_records c where c.campaign_id=p.id)",Long.class);
        if(missing>0)add(found,labels,"MISSING_CAMPAIGN_COST","CAMPAIGN","MULTIPLE","WARNING",missing+" campaigns have no cost records. ROI is unavailable, not zero.","MARK_ROI_UNAVAILABLE");
        long unverified=jdbc.queryForObject("select count(*) from web_sessions where contact_id is not null and identity_link_verified=false",Long.class);
        if(unverified>0)add(found,labels,"UNVERIFIED_IDENTITY_LINK","WEB_SESSION","MULTIPLE","ERROR",unverified+" web sessions link a contact without identity evidence.","REJECT_IDENTITY_LINK");
        issues.saveAll(found);truth.saveAll(labels);
        return Map.of("issueCount",found.size(),"groundTruthCount",labels.size(),
            "issues",found.stream().map(i->Map.of("id",i.getId(),"type",i.getIssueType(),"severity",i.getSeverity(),"description",i.getDescription())).toList());
    }
    private void add(List<DataQualityIssue> found,List<GroundTruth> labels,String type,String entity,String id,String severity,String description,String decision){
        String key=type+"-"+id;
        found.add(new DataQualityIssue(key,type,entity,id,severity,description,decision,Instant.now()));
        labels.add(new GroundTruth("GT-"+key,type,entity,id,decision,"TEST"));
    }
}
