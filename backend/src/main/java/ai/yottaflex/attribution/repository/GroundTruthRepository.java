package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.GroundTruth;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GroundTruthRepository extends JpaRepository<GroundTruth,String> {}
