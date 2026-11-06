package ai.yottaflex.attribution.repository;

import ai.yottaflex.attribution.entity.Touchpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TouchpointRepository extends JpaRepository<Touchpoint,String> {
    List<Touchpoint> findByContactDatasetRunId(String datasetRunId);
}
