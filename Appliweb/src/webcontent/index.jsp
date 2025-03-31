<!-- page d'acceuil -->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Accueil</title>
</head>
<body>
    <h1>Bienvenue au Club</h1>
    <p>Présentation du club...</p>
    <a href="Serv?action=inscription">Inscription</a> | <a href="Serv?action=connexion">Connexion</a>
    <h2>Navigation</h2>
    <ul>
        <li><a href="Serv?action=ajouterRecette">Gestion des Recettes</a></li>
        <li><a href="Serv?action=ajouterEvenement">Gestion des Événements</a></li>
        <li><a href="Serv?action=forum">Forum de Discussion</a></li>
        <li><a href="Serv?action=admin">Administration</a></li>
    </ul>
    <h2>Dernières Recettes</h2>
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
</body>
</html>