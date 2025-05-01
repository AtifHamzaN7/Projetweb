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

    @ElementCollection
    private List<Integer> valNut = new ArrayList<>();

    @ManyToMany(mappedBy = "ingredients")
    private List<Recette> recettes = new ArrayList<>();

    

    public Ingredient() {
    }

    public Ingredient(int idIng, String nom, int calories, List<Integer> valNut) {
        this.idIng = idIng;
        this.nom = nom;
        this.calories = calories;
        this.valNut = valNut;
    }

    // Constructeur simplifié (nom uniquement)
    public Ingredient(String nom) {
        this.idIng = 0; // Valeur par défaut
        this.nom = nom;
        this.calories = 0; // Valeur par défaut
        this.valNut = null; // Valeur par défaut
    }

    // Getters et setters
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

    public List<Integer> getValNut() {
        return valNut;
    }

    public void setValNut(List<Integer> valNut) {
        this.valNut = valNut;
    }

    public void setRecettes(List<Recette> recettes) {
        this.recettes = recettes;
        for (Recette recette : recettes) {
            if (!recette.getIngredients().contains(this)) {
                recette.getIngredients().add(this);
            }
        }
    }

    public void addValNut(int val) {
        if (valNut == null) {
            valNut = new ArrayList<>();
        }
        valNut.add(val);
    }

    
}