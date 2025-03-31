<!--Tableau du bord membre -->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Tableau de Bord</title>
</head>
<body>
    <h1>Bienvenue, <%= request.getAttribute("username") %></h1>
    <h2>Recettes Partagées</h2>
    <ul>
        <% 
            List<Recette> recettes = (List<Recette>) request.getAttribute("recettes");
            if (recettes != null) {
                for (Recette recette : recettes) {
        %>
            <li><%= recette.getNom() %></li>
        <% 
                }
            }
        %>
    </ul>
    <h2>Prochains Événements</h2>
    <ul>
        <% 
            List<Event> evenements = (List<Event>) request.getAttribute("evenements");
            if (evenements != null) {
                for (Event event : evenements) {
        %>
            <li><%= event.getTitre() %> - <%= event.getDate() %></li>
        <% 
                }
            }
        %>
    </ul>
    <h2>Actions Disponibles</h2>
    <ul>
        <li><a href="Serv?action=ajouterRecette">Gérer les Recettes</a></li>
        <li><a href="Serv?action=ajouterEvenement">Gérer les Événements</a></li>
        <li><a href="Serv?action=forum">Accéder au Forum</a></li>
        <%-- Lien vers l'administration, visible uniquement pour les administrateurs --%>
        <% 
            Adherent user = (Adherent) request.getAttribute("user");
            if (user != null && user.isAdmin()) { 
        %>
            <li><a href="Serv?action=admin">Administration</a></li>
        <% } %>
    </ul>
    <a href="Serv?action=forum">Accéder au Forum</a>
</body>
</html>