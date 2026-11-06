package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="opportunities")
public class Opportunity {
    @Id @Column(length=30) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="lead_id",nullable=false) private Lead lead;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="closed_at",nullable=false) private Instant closedAt;
    @Column(nullable=false,length=20) private String stage;
    @Column(name="expected_revenue",nullable=false,precision=16,scale=2) private BigDecimal expectedRevenue;
    @Column(nullable=false,length=3) private String currency;
    protected Opportunity(){}
    public Opportunity(String id,Lead lead,Instant createdAt,Instant closedAt,String stage,BigDecimal expectedRevenue,String currency){this.id=id;this.lead=lead;this.createdAt=createdAt;this.closedAt=closedAt;this.stage=stage;this.expectedRevenue=expectedRevenue;this.currency=currency;}
    public String getId(){return id;} public Lead getLead(){return lead;} public Instant getClosedAt(){return closedAt;} public BigDecimal getExpectedRevenue(){return expectedRevenue;}
}
