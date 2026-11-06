package ai.yottaflex.attribution.repository;
import ai.yottaflex.attribution.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContactRepository extends JpaRepository<Contact,String> {}
