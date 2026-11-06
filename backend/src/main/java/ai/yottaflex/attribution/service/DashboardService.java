package ai.yottaflex.attribution.service;

import ai.yottaflex.attribution.repository.*;
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
        for(String table:List.of("campaigns","contacts","touchpoints","web_sessions","leads","opportunities","orders","cost_records","attribution_results","data_quality_issues"))
            counts.put(table,jdbc.queryForObject("select count(*) from "+table,Long.class));
        List<Map<String,Object>> roi=campaignRoi("LINEAR");
        BigDecimal revenue=jdbc.queryForObject("select coalesce(sum(revenue),0) from orders",BigDecimal.class);
        BigDecimal spend=jdbc.queryForObject("select coalesce(sum(amount),0) from cost_records c join campaigns p on p.id=c.campaign_id where c.currency=p.currency",BigDecimal.class);
        return Map.of("counts",counts,"recognizedRevenue",revenue,"reconciledSpend",spend,"campaignRoi",roi);
    }

    public List<Map<String,Object>> campaignRoi(String model){
        String sql="""
          select p.id campaign_id,p.name campaign_name,p.currency,
          coalesce(s.spend,0) spend,coalesce(r.attributed_revenue,0) attributed_revenue,
          case when s.spend is null then 'UNAVAILABLE'
               when s.currency<>p.currency then 'CURRENCY_MISMATCH'
               when s.spend=0 then 'UNAVAILABLE'
               else 'CALCULATED' end roi_status,
          case when s.spend is null or s.currency<>p.currency or s.spend=0 then null
               else round(((coalesce(r.attributed_revenue,0)-s.spend)/s.spend)*100,2) end roi_percent
          from campaigns p
          left join (select campaign_id,sum(amount) spend,max(currency) currency from cost_records group by campaign_id) s on s.campaign_id=p.id
          left join (select t.campaign_id,sum(a.attributed_revenue) attributed_revenue
                     from attribution_results a join touchpoints t on t.id=a.touchpoint_id
                     where a.model_name=? group by t.campaign_id) r on r.campaign_id=p.id
          order by coalesce(r.attributed_revenue,0) desc
          """;
        return jdbc.queryForList(sql,model);
    }

    public List<Map<String,Object>> attributionByModel(){
        return jdbc.queryForList("""
          select model_name,sum(attributed_revenue) attributed_revenue,
          count(distinct opportunity_id) opportunities,count(*) credits
          from attribution_results group by model_name order by model_name
          """);
    }
}
