package pack;

import java.util.ArrayList;
import java.util.List;

public class Recette {
    int idRec;
    String nom;
    List<Ingredient> ingredients;
    List<String> etapes;
    String photo;
    String auteur;

    public Recette(int idRec, String nom, List<Ingredient> ingredients, List<String> etapes, String photo, String auteur) {
        this.idRec = idRec;
        this.nom = nom;
        this.ingredients = ingredients;
        this.etapes = etapes;
        this.photo = photo;
        this.auteur = auteur;
    }

    public int getidRec() {
        return idRec;
    }

    public String getNom() {
        return nom;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public List<String> getEtapes() {
        return etapes;
    }

    public String getPhoto() {
        return photo;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setidRec(int idRec) {
        this.idRec = idRec;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public void setEtapes(List<String> etapes) {
        this.etapes = etapes;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public void addIngredient(Ingredient ingredient) {
    if (ingredients == null) {
        ingredients = new ArrayList<>();
    }
    ingredients.add(ingredient);
    }

    public void addEtape(String etape) {
        if (etapes == null) {
            etapes = new ArrayList<>();
        }
        etapes.add(etape);
    }
}
