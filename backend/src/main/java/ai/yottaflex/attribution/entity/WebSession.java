package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="web_sessions")
public class WebSession {
    @Id @Column(length=80) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id") private Contact contact;
    @Column(name="anonymous_id",length=80) private String anonymousId;
    @Column(name="started_at",nullable=false) private Instant startedAt;
    @Column(name="landing_path",nullable=false,length=100) private String landingPath;
    @Column(name="identity_link_verified",nullable=false) private boolean identityLinkVerified;
    @Column(name="dataset_run_id",nullable=false,length=40) private String datasetRunId;
    protected WebSession(){}
    public WebSession(String id,Contact contact,String anonymousId,Instant startedAt,String landingPath,boolean verified){
        this(id,contact,anonymousId,startedAt,landingPath,verified,contact==null?"LEGACY":contact.getDatasetRunId());
    }
    public WebSession(String id,Contact contact,String anonymousId,Instant startedAt,String landingPath,boolean verified,String datasetRunId){
        this.id=id;this.contact=contact;this.anonymousId=anonymousId;this.startedAt=startedAt;this.landingPath=landingPath;this.identityLinkVerified=verified;this.datasetRunId=datasetRunId;
    }
    public String getId(){return id;} public Contact getContact(){return contact;} public String getAnonymousId(){return anonymousId;} public boolean isIdentityLinkVerified(){return identityLinkVerified;} public Instant getStartedAt(){return startedAt;} public String getDatasetRunId(){return datasetRunId;}
}
