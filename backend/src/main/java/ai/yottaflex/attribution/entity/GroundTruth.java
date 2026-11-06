package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;

@Entity
@Table(name="ground_truth")
public class GroundTruth {
    @Id @Column(length=40) private String id;
    @Column(name="issue_type",nullable=false,length=50) private String issueType;
    @Column(name="entity_type",nullable=false,length=40) private String entityType;
    @Column(name="entity_id",nullable=false,length=110) private String entityId;
    @Column(name="expected_decision",nullable=false,length=100) private String expectedDecision;
    @Column(name="evaluation_split",nullable=false,length=20) private String evaluationSplit;
    @Column(name="dataset_run_id",length=40) private String datasetRunId;

    protected GroundTruth(){}

    public GroundTruth(String id,String issueType,String entityType,String entityId,
            String expectedDecision,String evaluationSplit) {
        this(id,issueType,entityType,entityId,expectedDecision,evaluationSplit,null);
    }

    public GroundTruth(String id,String issueType,String entityType,String entityId,
            String expectedDecision,String evaluationSplit,String datasetRunId) {
        this.id=id;this.issueType=issueType;this.entityType=entityType;this.entityId=entityId;
        this.expectedDecision=expectedDecision;this.evaluationSplit=evaluationSplit;this.datasetRunId=datasetRunId;
    }

    public String getId(){return id;}
    public String getIssueType(){return issueType;}
    public String getExpectedDecision(){return expectedDecision;}
    public String getEvaluationSplit(){return evaluationSplit;}
    public String getDatasetRunId(){return datasetRunId;}
}
