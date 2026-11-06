package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="attribution_results",indexes=@Index(name="idx_attr_model",columnList="model_name,opportunity_id"))
public class AttributionResult {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="opportunity_id",nullable=false) private Opportunity opportunity;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="touchpoint_id",nullable=false) private Touchpoint touchpoint;
    @Column(name="model_name",nullable=false,length=20) private String modelName;
    @Column(nullable=false,precision=9,scale=6) private BigDecimal credit;
    @Column(name="attributed_revenue",nullable=false,precision=16,scale=2) private BigDecimal attributedRevenue;
    @Column(name="calculated_at",nullable=false) private Instant calculatedAt;
    protected AttributionResult(){}
    public AttributionResult(String id,Opportunity opportunity,Touchpoint touchpoint,String modelName,BigDecimal credit,BigDecimal attributedRevenue,Instant calculatedAt){this.id=id;this.opportunity=opportunity;this.touchpoint=touchpoint;this.modelName=modelName;this.credit=credit;this.attributedRevenue=attributedRevenue;this.calculatedAt=calculatedAt;}
}
