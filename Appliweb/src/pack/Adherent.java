package pack;

import java.util.List;

public class Adherent {
    int idAdh;
    String nom;
    String prenom;
    String email;
    String password;
    List<Ingredient> shoppingList;
    List<Ingredient> myIngredients;

    public Adherent(int idAdh, String nom, String prenom, String email, String password) {
        this.idAdh = idAdh;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
    }

    public int getidAdh() {
        return idAdh;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setidAdh(int idAdh) {
        this.idAdh = idAdh;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}
