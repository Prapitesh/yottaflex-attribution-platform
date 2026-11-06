package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name="campaign_audiences")
@IdClass(CampaignAudience.Key.class)
public class CampaignAudience {
    @Id
    @Column(name="campaign_id",nullable=false,length=30)
    private String campaignId;

    @Id
    @Column(name="audience_id",nullable=false,length=30)
    private String audienceId;

    @Id
    @Column(name="effective_from",nullable=false)
    private LocalDate effectiveFrom;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="campaign_id",insertable=false,updatable=false)
    private Campaign campaign;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="audience_id",insertable=false,updatable=false)
    private Audience audience;

    @Column(name="effective_to")
    private LocalDate effectiveTo;

    protected CampaignAudience() {}

    public CampaignAudience(Campaign campaign,Audience audience,LocalDate from,LocalDate to) {
        this.campaign=campaign;
        this.audience=audience;
        this.campaignId=campaign.getId();
        this.audienceId=audience.getId();
        this.effectiveFrom=from;
        this.effectiveTo=to;
    }

    public static class Key implements Serializable {
        public String campaignId;
        public String audienceId;
        public LocalDate effectiveFrom;
        public Key() {}

        @Override public boolean equals(Object other) {
            if(this==other)return true;
            if(!(other instanceof Key key))return false;
            return Objects.equals(campaignId,key.campaignId)
                &&Objects.equals(audienceId,key.audienceId)
                &&Objects.equals(effectiveFrom,key.effectiveFrom);
        }
        @Override public int hashCode() {
            return Objects.hash(campaignId,audienceId,effectiveFrom);
        }
    }
}
