package pack;
import java.util.List;

public class Ingredient {
    int idIng;
    String nom;
    int Calories;
    List<Integer> ValNut;

    public Ingredient(int idIng, String nom, int Calories, List<Integer> ValNut) {
        this.idIng = idIng;
        this.nom = nom;
        this.Calories = Calories;
        this.ValNut = ValNut;
    }

    public int getidIng() {
        return idIng;
    }

    public String getNom() {
        return nom;
    }

    public int getCalories() {
        return Calories;
    }

    public List<Integer> getValNut() {
        return ValNut;
    }

    public void setidIng(int idIng) {
        this.idIng = idIng;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setCalories(int Calories) {
        this.Calories = Calories;
    }

    public void setValNut(List<Integer> ValNut) {
        this.ValNut = ValNut;
    }

}
