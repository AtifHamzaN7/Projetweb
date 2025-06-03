package n7.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
public class Facade {

    @Autowired
    AdherentRepository adherentRepository;

    @Autowired
    RecetteRepository recetteRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    IngredientRepository ingredientRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    DiscussionRepository discussionRepository;

    @Autowired
    MessageRepository messageRepository;

    // Inscription d'un adhérent
    @PostMapping("/adherents/inscription")
    public void inscriptionAdherent(
            @RequestParam("nom") String nom,
            @RequestParam("prenom") String prenom,
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        Adherent adherent = new Adherent();
        adherent.setNom(nom);
        adherent.setPrenom(prenom);
        adherent.setEmail(email);
        adherent.setPassword(password);
        adherentRepository.save(adherent);
    }

    // Récupérer tous les adhérents
    @GetMapping("/adherents")
    public List<Adherent> listeAdherents() {
        return adherentRepository.findAll();
    }

    // Mise à jour d'un adhérent
    @PostMapping("/adherents/mise-a-jour")
    public void miseAJourAdherent(
            @RequestParam("idAdh") int idAdh,
            @RequestParam("nom") String nom,
            @RequestParam("prenom") String prenom,
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        Adherent adherent = adherentRepository.findById(idAdh).orElse(null);
        if (adherent != null) {
            adherent.setNom(nom);
            adherent.setPrenom(prenom);
            adherent.setEmail(email);
            adherent.setPassword(password);
            adherentRepository.save(adherent);
        }
    }

    // Suppression d'un adhérent
    @DeleteMapping("/adherents/suppression/{idAdh}")
    public void suppressionAdherent(@PathVariable("idAdh") int idAdh) {
        adherentRepository.deleteById(idAdh);
    }

    // Récupérer un adhérent par son ID
    @GetMapping("/adherents/{idAdh}")
    public Adherent getAdherentById(@PathVariable("idAdh") int idAdh) {
        return adherentRepository.findById(idAdh).orElse(null);
    }

    // Connexion d'un adhérent
    @GetMapping("/adherents/connexion")
    public Adherent connexionAdherent(
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        return adherentRepository.findByEmailAndPassword(email, password);
    }

    // Ajouter une recette
    @PostMapping("/recettes/ajout")
public void ajoutRecette(
        @RequestParam("nom") String nom,
        @RequestParam("ingredients") String ingredientsStr, // Format attendu : "(nom,calories,quantite)"
        @RequestParam("etapes") List<String> etapes,
        @RequestParam(value = "photo", required = false) String photo,
        @RequestParam("auteurId") int auteurId,
        @RequestParam("categories") List<String> categories // Liste des catégories
) {
    Adherent auteur = adherentRepository.findById(auteurId).orElse(null);
    if (auteur == null) {
        throw new IllegalArgumentException("Auteur introuvable !");
    }

    // Convertir les chaînes en objets Ingredient et les sauvegarder
    List<Ingredient> ingredientList = List.of(ingredientsStr.split("\\),\\(")).stream()
            .map(ingredientStr -> {
                // Supprimer les parenthèses si présentes
                ingredientStr = ingredientStr.replace("(", "").replace(")", "");
                String[] parts = ingredientStr.split(","); // Séparer nom, calories et quantité
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Format d'ingrédient invalide : " + ingredientStr);
                }
                String nomIngredient = parts[0].trim();
                int calories = Integer.parseInt(parts[1].trim());
                String quantite = parts[2].trim();
                Ingredient ingredient = new Ingredient(nomIngredient);
                ingredient.setCalories(calories);
                ingredient.setQuantite(quantite);
                return ingredientRepository.save(ingredient); // Sauvegarder chaque ingrédient
            })
            .toList();

    // Créer la recette
    Recette recette = new Recette();
    recette.setNom(nom);
    recette.setIngredients(ingredientList);
    recette.setEtapes(etapes);
    recette.setPhoto(photo);
    recette.setAuteur(auteur);
    recette.setCategories(categories); // Ajouter les catégories
    auteur.addRecette(recette);
    recetteRepository.save(recette);
}

    @GetMapping("/adherents/{idAdh}/recettes")
    public List<Recette> getRecettesByAdherent(@PathVariable("idAdh") int idAdh) {
        Adherent adherent = adherentRepository.findById(idAdh).orElse(null);
        if (adherent == null) {
            throw new IllegalArgumentException("Adhérent introuvable !");
        }
        return recetteRepository.findByAuteur_IdAdh(idAdh);
    }

    // Récupérer toutes les recettes
    @GetMapping("/recettes")
    public List<Recette> listeRecettes() {
        return recetteRepository.findAll();
    }

    // Suppression d'une recette
    @DeleteMapping("/recettes/suppression/{idRec}")
    public void suppressionRecette(@PathVariable("idRec") int idRec) {
        recetteRepository.deleteById(idRec);
    }

    // Ajouter un événement
    @PostMapping("/evenements/ajout")
    public void ajoutEvenement(
            @RequestParam("titre") String titre,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestParam("lieu") String lieu,
            @RequestParam("description") String description,
            @RequestParam("auteurId") int auteurId) {
        Adherent auteur = adherentRepository.findById(auteurId).orElse(null);
        if (auteur == null) {
            throw new IllegalArgumentException("Auteur introuvable !");
        }

        Event event = new Event();
        event.setTitre(titre);
        event.setDate(date);
        event.setLieu(lieu);
        event.setDescription(description);
        event.getParticipants().add(auteur);
        event.setAuteur(auteur);
        auteur.addEvenement(event);
        eventRepository.save(event);
    }

    @GetMapping("/adherents/{idAdh}/evenements")
    public List<Event> getEvenementsByAdherent(@PathVariable("idAdh") int idAdh) {
        Adherent adherent = adherentRepository.findById(idAdh).orElse(null);
        if (adherent == null) {
            throw new IllegalArgumentException("Adhérent introuvable !");
        }
        return eventRepository.findByAuteur_IdAdh(idAdh);
    }

    // Récupérer tous les événements
    @GetMapping("/evenements")
    public List<Event> listeEvenements() {
        return eventRepository.findAll();
    }

// Récupérer un événement par son ID
    @GetMapping("/evenements/{id}")
public Event getEventById(@PathVariable("id") int id) {
    return eventRepository.findById(id).orElse(null);
}

    // Suppression d'un événement
    @DeleteMapping("/evenements/suppression/{idEvent}")
    public void suppressionEvenement(@PathVariable("idEvent") int idEvent) {
        eventRepository.deleteById(idEvent);
    }

    @PostMapping("/evenements/{eventId}/participer")
public void participer(@PathVariable int eventId, @RequestParam int adherentId) {
    Event event = eventRepository.findById(eventId).orElseThrow();
    Adherent adherent = adherentRepository.findById(adherentId).orElseThrow();

    event.addParticipant(adherent);     
    eventRepository.save(event);        
}
    // Ajouter un commentaire à une recette
    @PostMapping("/recettes/{idRec}/commentaires/ajout")
    public void ajouterCommentaire(
            @PathVariable("idRec") int idRec,
            @RequestParam("auteurId") int auteurId,
            @RequestParam("content") String content) {
        Recette recette = recetteRepository.findById(idRec).orElse(null);
        Adherent auteur = adherentRepository.findById(auteurId).orElse(null);
        if (recette == null || auteur == null) {
            throw new IllegalArgumentException("Recette ou auteur introuvable !");
        }

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setAuteur(auteur);
        comment.setRecette(recette);
        commentRepository.save(comment);
    }

    // Récupérer les commentaires d'une recette
    @GetMapping("/recettes/{idRec}/commentaires")
    public List<Comment> getCommentairesByRecette(@PathVariable("idRec") int idRec) {
        return commentRepository.findByRecette_IdRec(idRec);
    }

    // Ajouter une discussion
    @PostMapping("/discussions/ajout")
    public void ajouterDiscussion(
        @RequestParam("titre") String titre,
        @RequestParam("question") String question,
        @RequestParam("auteurId") int auteurId
    ) {
        Adherent auteur = adherentRepository.findById(auteurId).orElse(null);
        if (auteur == null) {
            throw new IllegalArgumentException("Auteur introuvable !");
        }
        Discussion discussion = new Discussion();
        discussion.setTitre(titre);
        discussion.setQuestion(question);
        discussion.setAuteur(auteur);
        discussionRepository.save(discussion);
    }

    // Récupérer toutes les discussions
    @GetMapping("/discussions")
    public List<Discussion> listeDiscussions() {
        return discussionRepository.findAll();
    }

    // Récupérer une discussion par son ID
    @GetMapping("/discussions/{idDisc}")
    public Discussion getDiscussionById(@PathVariable("idDisc") int idDisc) {
        return discussionRepository.findById(idDisc).orElse(null);
    }

    // Ajouter un message à une discussion
    @PostMapping("/discussions/{idDisc}/messages/ajout")
    public void ajouterMessage(
        @PathVariable("idDisc") int idDisc,
        @RequestParam("auteurId") int auteurId,
        @RequestParam("content") String content
    ) {
        Discussion discussion = discussionRepository.findById(idDisc).orElse(null);
        Adherent auteur = adherentRepository.findById(auteurId).orElse(null);
        if (discussion == null || auteur == null) {
            throw new IllegalArgumentException("Discussion ou auteur introuvable !");
        }
        Message message = new Message();
        message.setContent(content);
        message.setAuteur(auteur);
        message.setDiscussion(discussion);
        messageRepository.save(message);
    }

    // Récupérer les messages d'une discussion
    @GetMapping("/discussions/{idDisc}/messages")
    public List<Message> getMessagesByDiscussion(@PathVariable("idDisc") int idDisc) {
        Discussion discussion = discussionRepository.findById(idDisc).orElse(null);
        if (discussion == null) {
            throw new IllegalArgumentException("Discussion introuvable !");
        }
        return discussion.getMessages();
    }
    
   @GetMapping("/images/{filename}")
public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
    Resource image = new ClassPathResource("static/" + filename);
    if (!image.exists()) {
        return ResponseEntity.notFound().build();
    }
    String contentType = filename.endsWith(".png") ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(image);
}
}