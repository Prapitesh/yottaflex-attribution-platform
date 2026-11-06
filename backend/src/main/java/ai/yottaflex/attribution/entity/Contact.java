package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="contacts")
public class Contact {
    @Id @Column(length=30) private String id;
    @Column(nullable=false,length=30) private String segment;
    @Column(nullable=false) private Instant createdAt;
    protected Contact(){}
    public Contact(String id,String segment,Instant createdAt){this.id=id;this.segment=segment;this.createdAt=createdAt;}
    public String getId(){return id;}
}
