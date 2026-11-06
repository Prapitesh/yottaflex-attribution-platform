package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="cost_records")
public class CostRecord {
    @Id @Column(length=30) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="campaign_id",nullable=false) private Campaign campaign;
    @Column(nullable=false) private LocalDate incurredOn;
    @Column(nullable=false,precision=16,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=3) private String currency;
    @Column(nullable=false,length=30) private String costType;
    protected CostRecord(){}
    public CostRecord(String id,Campaign campaign,LocalDate incurredOn,BigDecimal amount,String currency,String costType){this.id=id;this.campaign=campaign;this.incurredOn=incurredOn;this.amount=amount;this.currency=currency;this.costType=costType;}
}
