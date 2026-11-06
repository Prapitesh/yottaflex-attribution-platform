package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CampaignRepository extends JpaRepository<Campaign,String> {}
