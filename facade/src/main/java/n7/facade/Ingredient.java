package n7.facade;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idIng;

    private String nom;
    private int calories;
    private String quantite;

    @ManyToMany(mappedBy = "ingredients")
    private List<Recette> recettes = new ArrayList<>();

    

    public Ingredient() {
    }

    public Ingredient(int idIng, String nom, int calories) {
        this.idIng = idIng;
        this.nom = nom;
        this.calories = calories;
    }

    // Constructeur simplifié (nom uniquement)
    public Ingredient(String nom) {
        this.idIng = 0; // Valeur par défaut
        this.nom = nom;
        this.calories = 0; // Valeur par défaut
    }

    // Getters et setters

    public int getId() {
        return idIng;
    }

    public void setQuantite(String quantite) {
        this.quantite = quantite;
    }

    public String getQuantite() {
        return quantite;
    }
    public int getIdIng() {
        return idIng;
    }

    public void setIdIng(int idIng) {
        this.idIng = idIng;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public void setRecettes(List<Recette> recettes) {
        this.recettes = recettes;
        for (Recette recette : recettes) {
            if (!recette.getIngredients().contains(this)) {
                recette.getIngredients().add(this);
            }
        }
    }

  

    
}