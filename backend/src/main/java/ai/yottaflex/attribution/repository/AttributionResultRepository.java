package ai.yottaflex.attribution.repository;

import ai.yottaflex.attribution.entity.AttributionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttributionResultRepository extends JpaRepository<AttributionResult,String> {
    List<AttributionResult> findByModelName(String modelName);
}
