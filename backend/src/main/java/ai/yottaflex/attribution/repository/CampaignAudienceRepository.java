package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.CampaignAudience;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CampaignAudienceRepository extends JpaRepository<CampaignAudience,CampaignAudience.Key> {}
