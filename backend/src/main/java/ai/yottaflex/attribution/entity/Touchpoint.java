package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="touchpoints",indexes={
    @Index(name="idx_touch_contact_time",columnList="contact_id,event_at"),
    @Index(name="idx_touch_campaign",columnList="campaign_id")})
public class Touchpoint {
    @Id @Column(length=110) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id",nullable=false) private Contact contact;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="campaign_id",nullable=false) private Campaign campaign;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="creative_id") private Creative creative;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="channel_id",nullable=false) private Channel channel;
    @Column(name="event_at",nullable=false) private Instant eventAt;
    @Column(name="touch_type",nullable=false,length=20) private String touchType;
    @Column(name="source_type",nullable=false,length=20) private String sourceType;
    protected Touchpoint(){}
    public Touchpoint(String id,Contact contact,Campaign campaign,Creative creative,Channel channel,Instant eventAt,String touchType,String sourceType){this.id=id;this.contact=contact;this.campaign=campaign;this.creative=creative;this.channel=channel;this.eventAt=eventAt;this.touchType=touchType;this.sourceType=sourceType;}
    public String getId(){return id;} public Contact getContact(){return contact;} public Campaign getCampaign(){return campaign;} public Channel getChannel(){return channel;} public Instant getEventAt(){return eventAt;} public String getTouchType(){return touchType;} public String getSourceType(){return sourceType;}
}
