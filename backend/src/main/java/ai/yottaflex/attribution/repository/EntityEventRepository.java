package ai.yottaflex.attribution.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
public interface EntityEventRepository extends Repository<Object,Long> {
    @Query(value="select count(*) from entity_events",nativeQuery=true) long countEvents();
}
