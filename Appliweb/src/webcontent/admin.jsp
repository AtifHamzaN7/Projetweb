<!-- Page admin-->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Administration</title>
</head>
<body>
    <h1>Gestion des Membres</h1>
    <ul>
        <%-- Affichage des membres --%>
    </ul>
    <h2>Gestion des Recettes et Événements</h2>
    <ul>
        <%-- Affichage des recettes et événements --%>
    </ul>
    <h2>Envoi de Notifications</h2>
    <form action="Serv?action=envoyerNotification" method="post">
        <label>Message :</label><textarea name="message" required></textarea><br>
        <button type="submit">Envoyer</button>
    </form>
</body>
</html>