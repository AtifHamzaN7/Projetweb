package n7.facade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecetteRepository extends JpaRepository<Recette, Integer> {
    List<Recette> findByAuteur_IdAdh(int auteurId);
}