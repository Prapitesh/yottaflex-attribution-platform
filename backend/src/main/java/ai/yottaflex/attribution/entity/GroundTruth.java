package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;

@Entity
@Table(name="ground_truth")
public class GroundTruth {
    @Id @Column(length=40) private String id;
    @Column(nullable=false,length=50) private String issueType;
    @Column(nullable=false,length=40) private String entityType;
    @Column(nullable=false,length=60) private String entityId;
    @Column(nullable=false,length=100) private String expectedDecision;
    @Column(nullable=false,length=20) private String evaluationSplit;
    protected GroundTruth(){}
    public GroundTruth(String id,String issueType,String entityType,String entityId,String expectedDecision,String evaluationSplit){this.id=id;this.issueType=issueType;this.entityType=entityType;this.entityId=entityId;this.expectedDecision=expectedDecision;this.evaluationSplit=evaluationSplit;}
}
