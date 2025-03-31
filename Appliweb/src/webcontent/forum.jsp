<!-- Forum de discussion -->
<%@ page language="java" import="pack.*, java.util.*" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Forum</title>
</head>
<body>
    <h1>Forum de Discussion</h1>
    <ul>
        <% 
            List<Discussion> discussions = (List<Discussion>) request.getAttribute("discussions");
            if (discussions != null) {
                for (Discussion discussion : discussions) {
        %>
            <li><%= discussion.getTitre() %></li>
        <% 
                }
            }
        %>
    </ul>
    <h2>Poster un Message</h2>
    <form action="Serv" method="post">
        <input type="hidden" name="action" value="posterMessage">
        <label>Titre :</label><input type="text" name="titre" required><br>
        <label>Message :</label><textarea name="message" required></textarea><br>
        <button type="submit">Poster</button>
    </form>
</body>
</html>