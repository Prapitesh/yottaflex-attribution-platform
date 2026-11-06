package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OpportunityRepository extends JpaRepository<Opportunity,String> {}
