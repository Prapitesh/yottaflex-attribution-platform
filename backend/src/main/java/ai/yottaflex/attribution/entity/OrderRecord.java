package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="orders")
public class OrderRecord {
    @Id @Column(length=30) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="opportunity_id",nullable=false) private Opportunity opportunity;
    @Column(nullable=false,unique=true,length=60) private String externalRevenueKey;
    @Column(nullable=false,precision=16,scale=2) private BigDecimal revenue;
    @Column(nullable=false,length=3) private String currency;
    @Column(nullable=false) private Instant recognizedAt;
    protected OrderRecord(){}
    public OrderRecord(String id,Opportunity opportunity,String externalRevenueKey,BigDecimal revenue,String currency,Instant recognizedAt){this.id=id;this.opportunity=opportunity;this.externalRevenueKey=externalRevenueKey;this.revenue=revenue;this.currency=currency;this.recognizedAt=recognizedAt;}
}
