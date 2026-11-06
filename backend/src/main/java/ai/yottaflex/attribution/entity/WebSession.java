package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="web_sessions")
public class WebSession {
    @Id @Column(length=30) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id") private Contact contact;
    @Column(length=80) private String anonymousId;
    @Column(nullable=false) private Instant startedAt;
    @Column(nullable=false,length=100) private String landingPath;
    @Column(nullable=false) private boolean identityLinkVerified;
    protected WebSession(){}
    public WebSession(String id,Contact contact,String anonymousId,Instant startedAt,String landingPath,boolean identityLinkVerified){this.id=id;this.contact=contact;this.anonymousId=anonymousId;this.startedAt=startedAt;this.landingPath=landingPath;this.identityLinkVerified=identityLinkVerified;}
}
