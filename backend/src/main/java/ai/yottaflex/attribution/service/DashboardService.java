package ai.yottaflex.attribution.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;
    public DashboardService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Map<String,Object> summary(){
        Map<String,Object> counts=new LinkedHashMap<>();
        for(String table:List.of("campaigns","contacts","touchpoints","web_sessions","leads","opportunities","orders","cost_records","attribution_results","data_quality_issues","entity_events"))
            counts.put(table,jdbc.queryForObject("select count(*) from "+table,Long.class));
        BigDecimal revenue=jdbc.queryForObject("select coalesce(sum(revenue),0) from (select distinct on(external_revenue_key) external_revenue_key,revenue from orders order by external_revenue_key,recognized_at,id) x",BigDecimal.class);
        BigDecimal spend=jdbc.queryForObject("select coalesce(sum(c.amount),0) from cost_records c join campaigns p on p.id=c.campaign_id where c.currency=p.currency",BigDecimal.class);
        return Map.of("counts",counts,"recognizedRevenue",revenue,"reconciledSpend",spend,"campaignRoi",campaignRoi("LINEAR"));
    }
    public List<Map<String,Object>> campaignRoi(String model){
        return jdbc.queryForList("""
          with spend as (select campaign_id,sum(amount) amount,min(currency) currency from cost_records group by campaign_id),
          attributed as (select t.campaign_id,sum(a.attributed_revenue) amount from attribution_results a
            join touchpoints t on t.id=a.touchpoint_id where a.model_name=? group by t.campaign_id)
          select p.id campaign_id,p.name campaign_name,p.currency,
            case when s.currency=p.currency then coalesce(s.amount,0) else 0 end spend,
            coalesce(r.amount,0) attributed_revenue,
            case when s.campaign_id is null then 'UNAVAILABLE' when s.currency<>p.currency then 'CURRENCY_MISMATCH'
                 when s.amount=0 then 'UNAVAILABLE' else 'CALCULATED' end roi_status,
            case when s.campaign_id is null or s.currency<>p.currency or s.amount=0 then null
                 else round(((coalesce(r.amount,0)-s.amount)/s.amount)*100,2) end roi_percent
          from campaigns p left join spend s on s.campaign_id=p.id left join attributed r on r.campaign_id=p.id
          order by coalesce(r.amount,0) desc
          """,model);
    }
    public List<Map<String,Object>> attributionByModel(String model){
        return jdbc.queryForList("""
          select model_name,sum(attributed_revenue) attributed_revenue,
          count(distinct opportunity_id) opportunities,count(*) credits
          from attribution_results where model_name=? group by model_name
          """,model);
    }
}
