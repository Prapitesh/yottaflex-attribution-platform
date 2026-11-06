package ai.yottaflex.attribution.repository;

import ai.yottaflex.attribution.entity.DataQualityIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataQualityIssueRepository extends JpaRepository<DataQualityIssue,String> {
    List<DataQualityIssue> findAllByOrderByDetectedAtDesc();
    List<DataQualityIssue> findByResolvedFalseOrderByDetectedAtDesc();
}
