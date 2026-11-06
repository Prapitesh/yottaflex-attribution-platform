package ai.yottaflex.attribution.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="data_quality_issues")
public class DataQualityIssue {
    @Id @Column(length=40) private String id;
    @Column(name="issue_type",nullable=false,length=50) private String issueType;
    @Column(name="entity_type",nullable=false,length=40) private String entityType;
    @Column(name="entity_id",nullable=false,length=110) private String entityId;
    @Column(nullable=false,length=15) private String severity;
    @Column(nullable=false,columnDefinition="text") private String description;
    @Column(name="ground_truth_label",nullable=false,length=40) private String groundTruthLabel;
    @Column(name="detected_at",nullable=false) private Instant detectedAt;
    @Column(nullable=false) private boolean resolved;
    @Column(name="dataset_run_id",length=40) private String datasetRunId;

    protected DataQualityIssue(){}

    public DataQualityIssue(String id,String issueType,String entityType,String entityId,
            String severity,String description,String groundTruthLabel,Instant detectedAt) {
        this(id,issueType,entityType,entityId,severity,description,groundTruthLabel,detectedAt,null);
    }

    public DataQualityIssue(String id,String issueType,String entityType,String entityId,
            String severity,String description,String groundTruthLabel,Instant detectedAt,String datasetRunId) {
        this.id=id;this.issueType=issueType;this.entityType=entityType;this.entityId=entityId;
        this.severity=severity;this.description=description;this.groundTruthLabel=groundTruthLabel;
        this.detectedAt=detectedAt;this.datasetRunId=datasetRunId;this.resolved=false;
    }

    public String getId(){return id;}
    public String getIssueType(){return issueType;}
    public String getSeverity(){return severity;}
    public String getDescription(){return description;}
    public String getDatasetRunId(){return datasetRunId;}
}
