<!-- Gestion des Evénements -->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Gestion des Événements</title>
</head>
<body>
    <h1>Créer un Événement</h1>
    <form action="Serv" method="post">
        <input type="hidden" name="action" value="ajouterEvenement">
        <label>Titre :</label><input type="text" name="titre" required><br>
        <label>Date :</label><input type="date" name="date" required><br>
        <button type="submit">Créer</button>
    </form>
    <h2>Liste des Événements</h2>
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