package pack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/Serv")
public class Serv extends HttpServlet {

    private List<Adherent> adherents = new ArrayList<>();
    private List<Recette> recettes = new ArrayList<>();
    private List<Event> evenements = new ArrayList<>();
    private List<Discussion> discussions = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        switch (action) {
            case "inscription":
                handleInscription(request, response);
                break;
            case "connexion":
                handleConnexion(request, response);
                break;
            case "dashboard":
                handleDashboard(request, response);
                break;
            case "ajouterRecette":
                handleAjouterRecette(request, response);
                break;
            case "ajouterEvenement":
                handleAjouterEvenement(request, response);
                break;
            case "forum":
                handleForum(request, response);
                break;
            case "admin":
                handleAdmin(request, response);
                break;
            default:
                response.getWriter().println("Action non reconnue !");
        }
    }

    // Gestion de l'inscription
    private void handleInscription(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String nom = request.getParameter("nom");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        Adherent adherent = new Adherent(adherents.size() + 1, nom, "", email, password);
        adherents.add(adherent);

        response.sendRedirect("connexion.jsp");
    }

    // Gestion de la connexion
    private void handleConnexion(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        Adherent adherent = adherents.stream()
                .filter(a -> a.getEmail().equals(email) && a.getPassword().equals(password))
                .findFirst()
                .orElse(null);

        if (adherent != null) {
            request.setAttribute("username", adherent.getNom());
            request.getRequestDispatcher("dashboard.jsp").forward(request, response);
        } else {
            response.getWriter().println("Email ou mot de passe incorrect !");
        }
    }

    // Gestion du tableau de bord
    private void handleDashboard(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("recettes", recettes);
        request.setAttribute("evenements", evenements);
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }

    // Gestion de l'ajout de recette
    private void handleAjouterRecette(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String nom = request.getParameter("nom");
        String ingredients = request.getParameter("ingredients");

        Recette recette = new Recette(recettes.size() + 1, nom, new ArrayList<>(), new ArrayList<>(), "", "Auteur");
        recettes.add(recette);

        response.sendRedirect("recettes.jsp");
    }

    // Gestion de l'ajout d'événement
    private void handleAjouterEvenement(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String titre = request.getParameter("titre");
        String date = request.getParameter("date");

        Event event = new Event(evenements.size() + 1, titre, null, "", "", "Auteur", new ArrayList<>());
        evenements.add(event);

        response.sendRedirect("evenements.jsp");
    }

    // Gestion du forum
    private void handleForum(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("discussions", discussions);
        request.getRequestDispatcher("forum.jsp").forward(request, response);
    }

    // Gestion de l'administration
    private void handleAdmin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Adherent user = (Adherent) request.getSession().getAttribute("user");
        if (user != null && user.getidAdh() == 0) { // Vérifie si l'utilisateur est l'administrateur
            request.setAttribute("adherents", adherents);
            request.setAttribute("recettes", recettes);
            request.setAttribute("evenements", evenements);
            request.getRequestDispatcher("admin.jsp").forward(request, response);
        } else {
            response.getWriter().println("Accès refusé !");
        }
    }
}