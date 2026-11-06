package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LeadRepository extends JpaRepository<Lead,String> {}
