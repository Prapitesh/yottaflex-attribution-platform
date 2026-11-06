package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="leads")
public class Lead {
    @Id @Column(length=30) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id",nullable=false) private Contact contact;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id") private WebSession session;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false,length=20) private String status;
    protected Lead(){}
    public Lead(String id,Contact contact,WebSession session,Instant createdAt,String status){this.id=id;this.contact=contact;this.session=session;this.createdAt=createdAt;this.status=status;}
    public String getId(){return id;} public Contact getContact(){return contact;} public WebSession getSession(){return session;} public Instant getCreatedAt(){return createdAt;}
}
