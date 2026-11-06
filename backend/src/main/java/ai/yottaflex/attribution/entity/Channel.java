package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "channels")
public class Channel {
    @Id @Column(length = 30) private String id;
    @Column(nullable = false, unique = true, length = 80) private String name;
    @Column(nullable = false, length = 30) private String channelType;
    protected Channel() {}
    public Channel(String id, String name, String channelType) { this.id=id; this.name=name; this.channelType=channelType; }
    public String getId(){return id;} public String getName(){return name;} public String getChannelType(){return channelType;}
}
