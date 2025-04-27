
package pack;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class Facade {
    private Connection con;

    public Facade() {
        try {
            String db_url = "jdbc:hsqldb:hsql://localhost/xdb";
            String db_user = "sa";
            Class.forName("org.hsqldb.jdbcDriver");
            con = DriverManager.getConnection(db_url, db_user, null);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    
    // Ajouter un adhérent
    public void ajouterAdherent(String nom, String prenom, String email, String password) {
        try {
            String query = "INSERT INTO Adherent (nom, prenom, email, password) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, nom);
            pstmt.setString(2, prenom);
            pstmt.setString(3, email);
            pstmt.setString(4, password);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Récupérer tous les adhérents
    public List<Adherent> listeAdherents() {
        List<Adherent> adherents = new ArrayList<>();
        try {
            String query = "SELECT * FROM Adherent";
            PreparedStatement pstmt = con.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Adherent adherent = new Adherent(
                    rs.getInt("idAdh"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("password")
                );
                adherents.add(adherent);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return adherents;
    }

    // Mettre à jour un adhérent
    public void mettreAJourAdherent(int idAdh, String nom, String prenom, String email, String password) {
        try {
            String query = "UPDATE Adherent SET nom = ?, prenom = ?, email = ?, password = ? WHERE idAdh = ?";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, nom);
            pstmt.setString(2, prenom);
            pstmt.setString(3, email);
            pstmt.setString(4, password);
            pstmt.setInt(5, idAdh);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Supprimer un adhérent
    public void supprimerAdherent(int idAdh) {
        try {
            String query = "DELETE FROM Adherent WHERE idAdh = ?";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, idAdh);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Récupérer un adhérent par son ID
    public Adherent getAdherentById(int idAdh) {
        Adherent adherent = null;
        try {
            String query = "SELECT * FROM Adherent WHERE idAdh = ?";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setInt(1, idAdh);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                adherent = new Adherent(
                    rs.getInt("idAdh"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("password")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return adherent;
    }

    // Récupérer un adhérent par email et mot de passe
    public Adherent getAdherentByEmailAndPassword(String email, String password) {
        Adherent adherent = null;
        try {
            String query = "SELECT * FROM Adherent WHERE email = ? AND password = ?";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                adherent = new Adherent(
                    rs.getInt("idAdh"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("email"),
                    rs.getString("password")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return adherent;
    }

    public void ajouterRecette(String nom, List<Ingredient> ingredients, List<String> etapes, String photo, String auteur) {
        try {
            // Extraire les noms des ingrédients
            List<String> ingredientNames = ingredients.stream()
                .map(Ingredient::getNom) // Récupérer uniquement les noms
                .toList();
    
            // Convertir les listes en chaînes (format JSON ou simple)
            String ingredientsString = ingredientNames.toString(); // Stocker les noms sous forme de chaîne
            String etapesString = etapes.toString(); // Stocker les étapes sous forme de chaîne
    
            String query = "INSERT INTO Recette (nom, ingredients, etapes, photo, auteur) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, nom);
            pstmt.setString(2, ingredientsString); // Stocker les noms des ingrédients
            pstmt.setString(3, etapesString); // Stocker les étapes
            pstmt.setString(4, photo);
            pstmt.setString(5, auteur);
            pstmt.executeUpdate();
            System.out.println("Recette ajoutée avec succès !");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de l'ajout de la recette : " + e.getMessage());
        }
    }

    // Ajouter un événement
public void ajouterEvenement(String titre, Date date, String lieu, String description, String auteur) {
    try {
        String query = "INSERT INTO Event (titre, date, lieu, description, auteur) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pstmt = con.prepareStatement(query);
        pstmt.setString(1, titre);
        pstmt.setDate(2, new java.sql.Date(date.getTime())); // Convertir java.util.Date en java.sql.Date
        pstmt.setString(3, lieu);
        pstmt.setString(4, description);
        pstmt.setString(5, auteur);
        pstmt.executeUpdate();
        System.out.println("Événement ajouté avec succès !");
    } catch (SQLException e) {
        e.printStackTrace();
        System.err.println("Erreur lors de l'ajout de l'événement : " + e.getMessage());
    }
}



}

