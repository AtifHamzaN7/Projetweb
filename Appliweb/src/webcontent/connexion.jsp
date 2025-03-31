<!-- Page de connexion -->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Connexion</title>
</head>
<body>
    <h1>Connexion</h1>
    <form action="Serv" method="post">
        <input type="hidden" name="action" value="connexion">
        <label>Email :</label><input type="email" name="email" required><br>
        <label>Mot de passe :</label><input type="password" name="password" required><br>
        <button type="submit">Se connecter</button>
    </form>
</body>
</html>