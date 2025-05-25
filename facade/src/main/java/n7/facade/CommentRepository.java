package n7.facade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    // Méthode pour trouver les commentaires par recette
    List<Comment> findByRecette_IdRec(int idRec);

    // Méthode pour trouver les commentaires par auteur
    //List<Comment> findByAuteurId(int auteurId);
}