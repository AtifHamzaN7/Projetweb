package n7.facade;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Recette {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idRec;

    private String nom;

    @OneToMany(mappedBy = "recette", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "recette_ingredients",
        joinColumns = @JoinColumn(name = "recette_id"),
        inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private List<Ingredient> ingredients = new ArrayList<>();

    @ElementCollection
    private List<String> etapes = new ArrayList<>();

    private String photo;

    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    private Adherent auteur;

    @ElementCollection
    private List<String> categories = new ArrayList<>(); // vegan, sans gluten, etc.

    public Recette() {
    }

    public Recette(int idRec, String nom, List<Ingredient> ingredients, List<String> etapes, String photo, Adherent auteur, List<String> categories) {
        this.idRec = idRec;
        this.nom = nom;
        this.ingredients = ingredients;
        this.etapes = etapes;
        this.photo = photo;
        this.auteur = auteur;
        this.categories = categories;
    }

    // Getters et setters
    public int getIdRec() {
        return idRec;
    }

    public void setIdRec(int idRec) {
        this.idRec = idRec;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<String> getEtapes() {
        return etapes;
    }

    public void setEtapes(List<String> etapes) {
        this.etapes = etapes;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Adherent getAuteur() {
        return auteur;
    }
    
    public void setAuteur(Adherent auteur) {
        this.auteur = auteur;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
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

    public void addCategory(String category) {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        categories.add(category);
   }
   public void addComment(Comment comment) {
    if (comments == null) {
        comments = new ArrayList<>();
    }
    comments.add(comment);
    comment.setRecette(this);
}

public List<Comment> getComments() {
    return comments;
}
public void setComments(List<Comment> comments) {
    this.comments = comments;
}
}