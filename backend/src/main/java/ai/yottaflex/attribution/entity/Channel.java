package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;

@Entity
@Table(name="channels")
public class Channel {
    @Id @Column(length=50) private String id;
    @Column(nullable=false,length=80) private String name;
    @Column(name="channel_type",nullable=false,length=30) private String channelType;
    @Column(name="dataset_run_id",nullable=false,length=40) private String datasetRunId;

    protected Channel() {}

    public Channel(String id,String name,String channelType) {
        this(id,name,channelType,"LEGACY");
    }

    public Channel(String id,String name,String channelType,String datasetRunId) {
        this.id=id;this.name=name;this.channelType=channelType;this.datasetRunId=datasetRunId;
    }
    public String getId(){return id;}
    public String getName(){return name;}
    public String getChannelType(){return channelType;}
    public String getDatasetRunId(){return datasetRunId;}
}
