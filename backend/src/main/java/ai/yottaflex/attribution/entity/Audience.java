package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;

@Entity
@Table(name="audiences")
public class Audience {
    @Id @Column(length=50) private String id;
    @Column(nullable=false,length=100) private String name;
    @Column(name="segment_rule",nullable=false,length=120) private String segmentRule;
    @Column(name="dataset_run_id",nullable=false,length=40) private String datasetRunId;

    protected Audience() {}

    public Audience(String id,String name,String segmentRule) {
        this(id,name,segmentRule,"LEGACY");
    }

    public Audience(String id,String name,String segmentRule,String datasetRunId) {
        this.id=id;this.name=name;this.segmentRule=segmentRule;this.datasetRunId=datasetRunId;
    }
    public String getId(){return id;}
    public String getName(){return name;}
    public String getSegmentRule(){return segmentRule;}
    public String getDatasetRunId(){return datasetRunId;}
}
