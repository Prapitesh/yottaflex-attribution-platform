package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;

@Entity
@Table(name="creatives")
public class Creative {
    @Id @Column(length=60) private String id;
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false,length=30) private String format;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="campaign_id",nullable=false) private Campaign campaign;
    protected Creative(){}
    public Creative(String id,String name,String format,Campaign campaign){this.id=id;this.name=name;this.format=format;this.campaign=campaign;}
    public String getId(){return id;} public String getName(){return name;} public String getFormat(){return format;} public Campaign getCampaign(){return campaign;}
}
