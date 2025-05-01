package n7.facade;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/Serv")
public class Serv extends HttpServlet {

    private Facade facade;

    @Override
    public void init() throws ServletException {
        super.init();
        facade = new Facade(); // Initialisation de la façade
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if (action == null || action.isEmpty()) {
            response.getWriter().println("Erreur : le paramètre 'action' est manquant !");
            return;
        }

        switch (action) {
            case "inscription":
                handleInscription(request, response);
                break;
            case "connexion":
                handleConnexion(request, response);
                break;
            case "ajouterRecette":
                handleAjouterRecette(request, response);
                break;
            case "ajouterEvenement":
                handleAjouterEvenement(request, response);
                break;
            case "listeAdherents":
                handleListeAdherents(request, response);
                break;
            case "listeRecettes":
                handleListeRecettes(request, response);
                break;
            case "listeEvenements":
                handleListeEvenements(request, response);
                break;
            default:
                response.getWriter().println("Action non reconnue !");
        }
    }

    // Gestion de l'inscription
    private void handleInscription(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String nom = request.getParameter("nom");
        String prenom = request.getParameter("prenom");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (nom == null || prenom == null || email == null || password == null) {
            response.getWriter().println("Erreur : tous les champs sont obligatoires !");
            return;
        }

        // Ajouter l'adhérent dans la base de données via la façade
        facade.inscriptionAdherent(nom, prenom, email, password);

        // Réponse simple pour indiquer que l'inscription a réussi
        response.getWriter().println("Inscription réussie pour : " + nom + " " + prenom);
    }

    // Gestion de la connexion
    private void handleConnexion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null) {
            response.getWriter().println("Erreur : email et mot de passe sont obligatoires !");
            return;
        }

        // Vérifier l'adhérent dans la base de données via la façade
        Adherent adherent = facade.connexionAdherent(email, password);

        if (adherent != null) {
            // Si l'adhérent existe, afficher un message de succès
            response.getWriter().println("Connexion réussie pour : " + adherent.getNom() + " " + adherent.getPrenom());
        } else {
            // Si l'adhérent n'existe pas, afficher un message d'erreur
            response.getWriter().println("Erreur : email ou mot de passe incorrect !");
        }
    }

    private void handleAjouterRecette(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String nom = request.getParameter("nom");
        String ingredients = request.getParameter("ingredients"); // Liste d'ingrédients sous forme de chaîne
        String etapes = request.getParameter("etapes"); // Liste d'étapes sous forme de chaîne
        String photo = request.getParameter("photo");
        int auteurId = Integer.parseInt(request.getParameter("auteurId")); // Utiliser l'ID de l'auteur
    
        if (nom == null || ingredients == null || etapes == null || photo == null || auteurId == 0) {
            response.getWriter().println("Erreur : tous les champs sont obligatoires !");
            return;
        }
    
        Adherent auteur = facade.getAdherentById(auteurId);
        if (auteur == null) {
            response.getWriter().println("Erreur : auteur introuvable !");
            return;
        }
    
        List<Ingredient> ingredientList = List.of(ingredients.split(",")).stream()
                .map(Ingredient::new) // Créer des objets Ingredient avec uniquement le nom
                .toList();
        List<String> ingredientNames = ingredientList.stream()
                .map(Ingredient::getNom) // Extraire les noms des ingrédients
                .toList();
        //facade.ajoutRecette(nom, String.join(",", ingredientNames), List.of(etapes.split(",")), photo, auteur.getIdAdh());
    
        response.getWriter().println("Recette ajoutée avec succès : " + nom);
    }

    private void handleAjouterEvenement(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String titre = request.getParameter("titre");
        String dateStr = request.getParameter("date"); // Date sous forme de chaîne
        String lieu = request.getParameter("lieu");
        String description = request.getParameter("description");
        int auteurId = Integer.parseInt(request.getParameter("auteurId")); // Utiliser l'ID de l'auteur
    
        if (titre == null || dateStr == null || lieu == null || description == null || auteurId == 0) {
            response.getWriter().println("Erreur : tous les champs sont obligatoires !");
            return;
        }
    
        Adherent auteur = facade.getAdherentById(auteurId);
        if (auteur == null) {
            response.getWriter().println("Erreur : auteur introuvable !");
            return;
        }
    
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr); // Convertir la chaîne en Date
            facade.ajoutEvenement(titre, date, lieu, description, auteur.getIdAdh());
    
            response.getWriter().println("Événement ajouté avec succès : " + titre);
        } catch (ParseException e) {
            response.getWriter().println("Erreur : format de date invalide !");
        }
    }

    // Gestion de la liste des adhérents
    private void handleListeAdherents(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Adherent> adherents = facade.listeAdherents();
        response.getWriter().println(adherents);
    }

    // Gestion de la liste des recettes
    private void handleListeRecettes(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Recette> recettes = facade.listeRecettes();
        response.getWriter().println(recettes);
    }

    // Gestion de la liste des événements
    private void handleListeEvenements(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Event> evenements = facade.listeEvenements();
        response.getWriter().println(evenements);
    }
}