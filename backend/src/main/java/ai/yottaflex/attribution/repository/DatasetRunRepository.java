package ai.yottaflex.attribution.repository;

import ai.yottaflex.attribution.entity.DatasetRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DatasetRunRepository extends JpaRepository<DatasetRun,String> {
    List<DatasetRun> findAllByOrderByCreatedAtDesc();
}
