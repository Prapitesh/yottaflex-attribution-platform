package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name="campaign_audiences")
@IdClass(CampaignAudience.Key.class)
public class CampaignAudience {
    @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="campaign_id") private Campaign campaign;
    @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="audience_id") private Audience audience;
    @Id @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
    @Column(name="effective_to") private LocalDate effectiveTo;
    protected CampaignAudience(){}
    public CampaignAudience(Campaign campaign,Audience audience,LocalDate from,LocalDate to){this.campaign=campaign;this.audience=audience;this.effectiveFrom=from;this.effectiveTo=to;}
    public static class Key implements Serializable {
        public String campaign;
        public String audience;
        public LocalDate effectiveFrom;
        public Key(){}
        @Override public boolean equals(Object other){
            if(this==other)return true;
            if(!(other instanceof Key key))return false;
            return Objects.equals(campaign,key.campaign)&&Objects.equals(audience,key.audience)&&Objects.equals(effectiveFrom,key.effectiveFrom);
        }
        @Override public int hashCode(){return Objects.hash(campaign,audience,effectiveFrom);}
    }
}
