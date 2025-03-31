<!-- Gestion de recettes -->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Gestion des Recettes</title>
</head>
<body>
    <h1>Ajouter une Recette</h1>
    <form action="Serv" method="post">
        <input type="hidden" name="action" value="ajouterRecette">
        <label>Nom :</label><input type="text" name="nom" required><br>
        <label>Ingrédients :</label><textarea name="ingredients" required></textarea><br>
        <button type="submit">Ajouter</button>
    </form>
    <h2>Liste des Recettes</h2>
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
</body>
</html>