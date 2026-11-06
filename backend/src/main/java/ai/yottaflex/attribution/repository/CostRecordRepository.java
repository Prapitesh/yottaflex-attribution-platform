package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.CostRecord;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CostRecordRepository extends JpaRepository<CostRecord,String> {}
