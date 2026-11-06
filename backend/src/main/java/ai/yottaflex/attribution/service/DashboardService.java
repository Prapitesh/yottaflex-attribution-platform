package ai.yottaflex.attribution.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;
    public DashboardService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public Map<String,Object> summary(String runId) {
        Map<String,Object> counts=new LinkedHashMap<>();
        for(String table:List.of("campaigns","contacts","touchpoints","web_sessions",
                "leads","opportunities","orders","cost_records","attribution_results",
                "data_quality_issues","entity_events")) {
            counts.put(table,countForRun(table,runId));
        }

        BigDecimal revenue=jdbc.queryForObject("""
            select coalesce(sum(dedup.revenue),0)
            from (
                select distinct on(o.external_revenue_key)
                    o.external_revenue_key,o.revenue
                from orders o
                join opportunities opp on opp.id=o.opportunity_id
                join leads l on l.id=opp.lead_id
                join contacts c on c.id=l.contact_id
                where (CAST(? AS VARCHAR) IS NULL or c.dataset_run_id=?)
                order by o.external_revenue_key,o.recognized_at,o.id
            ) dedup
            """,BigDecimal.class,runId,runId);

        BigDecimal spend=jdbc.queryForObject("""
            select coalesce(sum(cost.amount),0)
            from cost_records cost
            join campaigns campaign on campaign.id=cost.campaign_id
            where cost.currency=campaign.currency
              and (CAST(? AS VARCHAR) IS NULL or campaign.dataset_run_id=?)
            """,BigDecimal.class,runId,runId);

        return Map.of("runId",runId==null?"ALL":runId,
            "counts",counts,"recognizedRevenue",revenue,
            "reconciledSpend",spend,"campaignRoi",campaignRoi("LINEAR",runId));
    }

    private long countForRun(String table,String runId) {
        if(runId==null) {
            return jdbc.queryForObject("select count(*) from "+table,Long.class);
        }
        return switch(table) {
            case "campaigns","contacts","web_sessions" ->
                jdbc.queryForObject("select count(*) from "+table+" where dataset_run_id=?",Long.class,runId);
            case "touchpoints" ->
                jdbc.queryForObject("""
                    select count(*) from touchpoints t join contacts c on c.id=t.contact_id
                    where c.dataset_run_id=?
                    """,Long.class,runId);
            case "leads" ->
                jdbc.queryForObject("""
                    select count(*) from leads l join contacts c on c.id=l.contact_id
                    where c.dataset_run_id=?
                    """,Long.class,runId);
            case "opportunities" ->
                jdbc.queryForObject("""
                    select count(*) from opportunities o join leads l on l.id=o.lead_id
                    join contacts c on c.id=l.contact_id where c.dataset_run_id=?
                    """,Long.class,runId);
            case "orders" ->
                jdbc.queryForObject("""
                    select count(*) from orders x join opportunities o on o.id=x.opportunity_id
                    join leads l on l.id=o.lead_id join contacts c on c.id=l.contact_id
                    where c.dataset_run_id=?
                    """,Long.class,runId);
            case "cost_records" ->
                jdbc.queryForObject("""
                    select count(*) from cost_records x join campaigns c on c.id=x.campaign_id
                    where c.dataset_run_id=?
                    """,Long.class,runId);
            case "attribution_results" ->
                jdbc.queryForObject("""
                    select count(*) from attribution_results a
                    join opportunities o on o.id=a.opportunity_id
                    join leads l on l.id=o.lead_id join contacts c on c.id=l.contact_id
                    where c.dataset_run_id=?
                    """,Long.class,runId);
            case "entity_events" ->
                jdbc.queryForObject("""
                    select count(*) from entity_events e
                    where e.metadata->>'datasetRunId'=?
                       or e.entity_id like ?
                    """,Long.class,runId,"%"+runId+"%");
            case "data_quality_issues" ->
                jdbc.queryForObject("""
                    select count(*) from data_quality_issues q
                    where q.dataset_run_id=?
                    """,Long.class,runId);
            default -> throw new IllegalArgumentException("Unsupported dashboard table: "+table);
        };
    }

    public List<Map<String,Object>> campaignRoi(String model,String runId) {
        validateModel(model);
        return jdbc.queryForList("""
            with spend as (
                select cost.campaign_id,
                    sum(case when cost.currency=campaign.currency then cost.amount else 0 end) amount,
                    bool_or(cost.currency<>campaign.currency) has_currency_mismatch
                from cost_records cost
                join campaigns campaign on campaign.id=cost.campaign_id
                group by cost.campaign_id
            ),
            attributed as (
                select t.campaign_id,sum(a.attributed_revenue) amount
                from attribution_results a
                join touchpoints t on t.id=a.touchpoint_id
                join opportunities o on o.id=a.opportunity_id
                join leads l on l.id=o.lead_id
                join contacts c on c.id=l.contact_id
                where a.model_name=?
                  and (CAST(? AS VARCHAR) IS NULL or c.dataset_run_id=?)
                  and a.calculated_at=(
                    select max(a2.calculated_at)
                    from attribution_results a2
                    join opportunities o2 on o2.id=a2.opportunity_id
                    join leads l2 on l2.id=o2.lead_id
                    join contacts c2 on c2.id=l2.contact_id
                    where a2.model_name=?
                      and (CAST(? AS VARCHAR) IS NULL or c2.dataset_run_id=?)
                  )
                group by t.campaign_id
            )
            select p.id campaign_id,p.name campaign_name,p.currency,
                case when s.has_currency_mismatch then 0 else coalesce(s.amount,0) end spend,
                coalesce(r.amount,0) attributed_revenue,
                case when s.campaign_id is null then 'UNAVAILABLE'
                     when s.has_currency_mismatch then 'CURRENCY_MISMATCH'
                     when s.amount=0 then 'UNAVAILABLE'
                     else 'CALCULATED' end roi_status,
                case when s.campaign_id is null or s.has_currency_mismatch or s.amount=0 then null
                     else round(((coalesce(r.amount,0)-s.amount)/s.amount)*100,2)
                end roi_percent
            from campaigns p
            left join spend s on s.campaign_id=p.id
            left join attributed r on r.campaign_id=p.id
            where (CAST(? AS VARCHAR) IS NULL or p.dataset_run_id=?)
            order by coalesce(r.amount,0) desc
            """,model,runId,runId,model,runId,runId,runId,runId);
    }

    public List<Map<String,Object>> attributionByModel(String model,String runId) {
        validateModel(model);
        return jdbc.queryForList("""
            select a.model_name,sum(a.attributed_revenue) attributed_revenue,
                count(distinct a.opportunity_id) opportunities,count(*) credits
            from attribution_results a
            join opportunities o on o.id=a.opportunity_id
            join leads l on l.id=o.lead_id
            join contacts c on c.id=l.contact_id
            where a.model_name=?
              and (CAST(? AS VARCHAR) IS NULL or c.dataset_run_id=?)
              and a.calculated_at=(
                select max(a2.calculated_at)
                from attribution_results a2
                join opportunities o2 on o2.id=a2.opportunity_id
                join leads l2 on l2.id=o2.lead_id
                join contacts c2 on c2.id=l2.contact_id
                where a2.model_name=?
                  and (CAST(? AS VARCHAR) IS NULL or c2.dataset_run_id=?)
              )
            group by a.model_name
            """,model,runId,runId,model,runId,runId);
    }

    private void validateModel(String model) {
        if(!List.of("FIRST_TOUCH","LAST_TOUCH","LINEAR").contains(model)) {
            throw new IllegalArgumentException("Unsupported attribution model: "+model);
        }
    }
}
