package pack;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
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
        facade.ajouterAdherent(nom, prenom, email, password);

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
        Adherent adherent = facade.getAdherentByEmailAndPassword(email, password);

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
        String auteur = request.getParameter("auteur");
    
        if (nom == null || ingredients == null || etapes == null || photo == null || auteur == null) {
            response.getWriter().println("Erreur : tous les champs sont obligatoires !");
            return;
        }
    
        // Ajouter la recette dans la base de données via la façade
        List<Ingredient> ingredientList = List.of(ingredients.split(",")).stream()
            .map(Ingredient::new) // Créer des objets Ingredient avec uniquement le nom
            .toList();
        facade.ajouterRecette(nom, ingredientList, List.of(etapes.split(",")), photo, auteur);
    
        // Réponse simple pour indiquer que l'ajout a réussi
        response.getWriter().println("Recette ajoutée avec succès : " + nom);
    }

    // Gestion de l'ajout d'un événement
private void handleAjouterEvenement(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String titre = request.getParameter("titre");
    String dateStr = request.getParameter("date"); // Date sous forme de chaîne
    String lieu = request.getParameter("lieu");
    String description = request.getParameter("description");
    String auteur = request.getParameter("auteur");

    if (titre == null || dateStr == null || lieu == null || description == null || auteur == null) {
        response.getWriter().println("Erreur : tous les champs sont obligatoires !");
        return;
    }

    try {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr); // Convertir la chaîne en Date
        // Ajouter l'événement dans la base de données via la façade
        facade.ajouterEvenement(titre, date, lieu, description, auteur);

        // Réponse simple pour indiquer que l'ajout a réussi
        response.getWriter().println("Événement ajouté avec succès : " + titre);
    } catch (ParseException e) {
        response.getWriter().println("Erreur : format de date invalide !");
    }
}
}

