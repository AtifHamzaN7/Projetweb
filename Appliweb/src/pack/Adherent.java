package pack;

import java.util.ArrayList;
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
    if (shoppingList == null) {
        shoppingList = new ArrayList<>();
    }
    shoppingList.add(ingredient);
    }

    public void addIngredientToMyIngredients(Ingredient ingredient) {
        if (myIngredients == null) {
            myIngredients = new ArrayList<>();
        }
        myIngredients.add(ingredient);
    }

    public void removeIngredientFromShoppingList(Ingredient ingredient) {
        if (shoppingList != null) {
            shoppingList.remove(ingredient);
        }
    }

    public void removeIngredientFromMyIngredients(Ingredient ingredient) {
        if (myIngredients != null) {
            myIngredients.remove(ingredient);
        }
    }
    public void clearShoppingList() {
        if (shoppingList != null) {
            shoppingList.clear();
        }
    }

    public boolean isAdmin(){
        if (this.getidAdh() == 0){
            return true;
        }
        return false;
    }
}
