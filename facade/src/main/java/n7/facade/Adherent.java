package n7.facade;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Adherent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idAdh;

    private String nom;
    private String prenom;
    private String email;
    private String password;

    // Liste personnelle d'ingrédients
    @ManyToMany
    @JoinTable(
        name = "adherent_my_ingredients",
        joinColumns = @JoinColumn(name = "adherent_id"),
        inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private List<Ingredient> myIngredients = new ArrayList<>();

    // Liste de courses
    @ManyToMany
    @JoinTable(
        name = "adherent_shopping_list",
        joinColumns = @JoinColumn(name = "adherent_id"),
        inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private List<Ingredient> shoppingList = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "auteur", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Recette> recettes = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "auteur", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Event> evenements = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "auteur", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Discussion> discussions = new ArrayList<>();

    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
     @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Message> messages = new ArrayList<>();


    public Adherent() {
    }

    public Adherent(int idAdh, String nom, String prenom, String email, String password) {
        this.idAdh = idAdh;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
    }

    // Getters and setters
    public int getIdAdh() {
        return idAdh;
    }

    public void setIdAdh(int idAdh) {
        this.idAdh = idAdh;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Ingredient> getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(List<Ingredient> shoppingList) {
        this.shoppingList = shoppingList;
    }

    public List<Ingredient> getMyIngredients() {
        return myIngredients;
    }

    public void setMyIngredients(List<Ingredient> myIngredients) {
        this.myIngredients = myIngredients;
    }

    public void addIngredientToShoppingList(Ingredient ingredient) {
        if (!shoppingList.contains(ingredient)) {
            shoppingList.add(ingredient);
        }
    }

    public void addIngredientToMyIngredients(Ingredient ingredient) {
        if (!myIngredients.contains(ingredient)) {
            myIngredients.add(ingredient);
        }
    }

    public void addRecette(Recette recette) {
        if (recettes == null) {
            recettes = new ArrayList<>();
        }
        recettes.add(recette);
        recette.setAuteur(this); // Synchroniser l'autre côté
    }

    public void addEvenement(Event event) {
        if (evenements == null) {
            evenements = new ArrayList<>();
        }
        evenements.add(event);
        event.setAuteur(this); // Synchroniser l'autre côté
    }

    public void addDiscussion(Discussion discussion) {
        if (discussions == null) {
            discussions = new ArrayList<>();
        }
        discussions.add(discussion);
        discussion.setAuteur(this); // Synchroniser l'autre côté
    }
    public List<Event> getEvenements() {
        return evenements;
    }
    
    public List<Discussion> getDiscussions() {
        return discussions;
    }
    
    public List<Comment> getComments() {
        return comments;
    }
    
    public List<Message> getMessages() {
        return messages;
    }

    
    public void addComment(Comment comment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(comment);
        comment.setAuteur(this); // Synchroniser l'autre côté
    }

    public List<Recette> getRecettes() {
        return recettes;
    }

    
}