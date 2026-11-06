package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRecordRepository extends JpaRepository<OrderRecord,String> {}
