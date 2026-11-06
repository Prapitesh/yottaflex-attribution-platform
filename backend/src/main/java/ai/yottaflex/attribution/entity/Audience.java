package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;

@Entity
@Table(name="audiences")
public class Audience {
    @Id @Column(length=30) private String id;
    @Column(nullable=false,unique=true,length=100) private String name;
    @Column(nullable=false,length=120) private String segmentRule;
    protected Audience(){}
    public Audience(String id,String name,String segmentRule){this.id=id;this.name=name;this.segmentRule=segmentRule;}
    public String getId(){return id;} public String getName(){return name;}
}
