package ai.yottaflex.attribution.repository;

import ai.yottaflex.attribution.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity,String> {
    List<Opportunity> findByLeadContactDatasetRunId(String datasetRunId);
}
