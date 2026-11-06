package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="contacts")
public class Contact {
    @Id @Column(length=50) private String id;
    @Column(nullable=false,length=30) private String segment;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="dataset_run_id",nullable=false,length=40) private String datasetRunId;

    protected Contact() {}

    public Contact(String id,String segment,Instant createdAt) {
        this(id,segment,createdAt,"LEGACY");
    }

    public Contact(String id,String segment,Instant createdAt,String datasetRunId) {
        this.id=id;this.segment=segment;this.createdAt=createdAt;this.datasetRunId=datasetRunId;
    }
    public String getId(){return id;}
    public String getSegment(){return segment;}
    public Instant getCreatedAt(){return createdAt;}
    public String getDatasetRunId(){return datasetRunId;}
}
