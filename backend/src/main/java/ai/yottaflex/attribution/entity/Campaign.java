package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="campaigns")
public class Campaign {
    @Id @Column(length=50) private String id;
    @Column(nullable=false,length=120) private String name;
    @Column(name="dataset_run_id",nullable=false,length=40) private String datasetRunId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="channel_id",nullable=false) private Channel channel;
    @Column(nullable=false,precision=16,scale=2) private BigDecimal budget;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date",nullable=false) private LocalDate endDate;
    @Column(nullable=false,length=20) private String status;

    protected Campaign() {}

    public Campaign(String id,String name,Channel channel,BigDecimal budget,String currency,
            LocalDate startDate,LocalDate endDate,String status) {
        this(id,name,channel,budget,currency,startDate,endDate,status,
            channel.getDatasetRunId());
    }

    public Campaign(String id,String name,Channel channel,BigDecimal budget,String currency,
            LocalDate startDate,LocalDate endDate,String status,String datasetRunId) {
        this.id=id;this.name=name;this.channel=channel;this.budget=budget;this.currency=currency;
        this.startDate=startDate;this.endDate=endDate;this.status=status;this.datasetRunId=datasetRunId;
    }
    public String getId(){return id;}
    public String getName(){return name;}
    public Channel getChannel(){return channel;}
    public BigDecimal getBudget(){return budget;}
    public String getCurrency(){return currency;}
    public LocalDate getStartDate(){return startDate;}
    public LocalDate getEndDate(){return endDate;}
    public String getStatus(){return status;}
    public String getDatasetRunId(){return datasetRunId;}
}
