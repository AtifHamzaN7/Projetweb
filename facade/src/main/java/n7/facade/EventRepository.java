package n7.facade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findByAuteur_IdAdh(int auteurId);

@Query("SELECT e FROM Event e JOIN e.participants p WHERE p.idAdh = :idAdh")
List<Event> findByParticipantId(@Param("idAdh") int idAdh);
}

