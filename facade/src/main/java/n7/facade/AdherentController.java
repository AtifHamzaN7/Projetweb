package n7.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;


@CrossOrigin(origins = "http://localhost:4200") // Autorise les requêtes du frontend Angular
@RestController
@RequestMapping("/api/adherents") // Tous les endpoints sont préfixés par /api/adherents
public class AdherentController {

    @Autowired
    private AdherentRepository adherentRepository;

    // 👉 Créer un nouvel adhérent (inscription)
    @PostMapping
    public Adherent createAdherent(@RequestBody Adherent adherent) {
        return adherentRepository.save(adherent);
    }

    @PostMapping("/adherents/inscription")
public ResponseEntity<Adherent> inscriptionAdherent(
        @RequestParam("nom") String nom,
        @RequestParam("prenom") String prenom,
        @RequestParam("email") String email,
        @RequestParam("password") String password) {
    Adherent adherent = new Adherent();
    adherent.setNom(nom);
    adherent.setPrenom(prenom);
    adherent.setEmail(email);
    adherent.setPassword(password);
    Adherent saved = adherentRepository.save(adherent);
    return ResponseEntity.ok(saved); // ✅ retourne l'objet sauvegardé
}


    // 👉 Obtenir tous les adhérents
    @GetMapping
    public List<Adherent> getAllAdherents() {
        return adherentRepository.findAll();
    }

    // 👉 Connexion : chercher un adhérent par email et mot de passe
    @GetMapping("/connexion")
    public Adherent login(@RequestParam String email, @RequestParam String password) {
        return adherentRepository.findByEmailAndPassword(email, password);
    }

    // 👉 Récupérer un adhérent par ID
    @GetMapping("/{id}")
    public Adherent getAdherentById(@PathVariable("id") int id) {
        return adherentRepository.findById(id).orElse(null);
    }

    // 👉 Supprimer un adhérent
    @DeleteMapping("/{id}")
    public void deleteAdherent(@PathVariable("id") int id) {
        adherentRepository.deleteById(id);
    }

    // 👉 Mettre à jour un adhérent
    @PutMapping("/{id}")
    public Adherent updateAdherent(@PathVariable("id") int id, @RequestBody Adherent updatedAdherent) {
        return adherentRepository.findById(id).map(adherent -> {
            adherent.setNom(updatedAdherent.getNom());
            adherent.setPrenom(updatedAdherent.getPrenom());
            adherent.setEmail(updatedAdherent.getEmail());
            adherent.setPassword(updatedAdherent.getPassword());
            return adherentRepository.save(adherent);
        }).orElse(null);
    }
}
