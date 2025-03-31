<!-- Page d'inscription -->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Inscription</title>
</head>
<body>
    <h1>Inscription</h1>
    <form action="Serv" method="post">
        <input type="hidden" name="action" value="inscription">
        <label>Nom :</label><input type="text" name="nom" required><br>
        <label>Email :</label><input type="email" name="email" required><br>
        <label>Mot de passe :</label><input type="password" name="password" required><br>
        <button type="submit">S'inscrire</button>
    </form>
</body>
</html>