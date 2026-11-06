package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="orders",indexes=@Index(name="idx_order_revenue_key",columnList="external_revenue_key"))
public class OrderRecord {
    @Id @Column(length=30) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="opportunity_id",nullable=false) private Opportunity opportunity;
    @Column(name="external_revenue_key",nullable=false,length=60) private String externalRevenueKey;
    @Column(nullable=false,precision=16,scale=2) private BigDecimal revenue;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="recognized_at",nullable=false) private Instant recognizedAt;
    protected OrderRecord(){}
    public OrderRecord(String id,Opportunity opportunity,String externalRevenueKey,BigDecimal revenue,String currency,Instant recognizedAt){this.id=id;this.opportunity=opportunity;this.externalRevenueKey=externalRevenueKey;this.revenue=revenue;this.currency=currency;this.recognizedAt=recognizedAt;}
    public String getId(){return id;} public Opportunity getOpportunity(){return opportunity;} public String getExternalRevenueKey(){return externalRevenueKey;} public BigDecimal getRevenue(){return revenue;} public Instant getRecognizedAt(){return recognizedAt;}
}
