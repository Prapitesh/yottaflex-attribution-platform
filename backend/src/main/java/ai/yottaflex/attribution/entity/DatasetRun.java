package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="dataset_runs")
public class DatasetRun {
    @Id
    @Column(name="id",length=40)
    private String id;

    @Column(nullable=false)
    private long seed;

    @Column(name="campaign_count",nullable=false)
    private int campaignCount;

    @Column(name="contact_count",nullable=false)
    private int contactCount;

    @Column(name="touchpoint_count",nullable=false)
    private int touchpointCount;

    @Column(name="opportunity_count",nullable=false)
    private int opportunityCount;

    @Column(name="created_at",nullable=false)
    private Instant createdAt;

    @Column(nullable=false,length=20)
    private String status;

    protected DatasetRun(){}

    public DatasetRun(String id,long seed,int campaigns,int contacts,int touches,
            int opportunities,Instant createdAt) {
        this.id=id;
        this.seed=seed;
        this.campaignCount=campaigns;
        this.contactCount=contacts;
        this.touchpointCount=touches;
        this.opportunityCount=opportunities;
        this.createdAt=createdAt;
        this.status="GENERATED";
    }

    public String getId(){return id;}
    public long getSeed(){return seed;}
    public int getCampaignCount(){return campaignCount;}
    public int getContactCount(){return contactCount;}
    public int getTouchpointCount(){return touchpointCount;}
    public int getOpportunityCount(){return opportunityCount;}
    public Instant getCreatedAt(){return createdAt;}
    public String getStatus(){return status;}
}
