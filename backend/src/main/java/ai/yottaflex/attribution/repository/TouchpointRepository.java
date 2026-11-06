package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.Touchpoint;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TouchpointRepository extends JpaRepository<Touchpoint,String> {}
