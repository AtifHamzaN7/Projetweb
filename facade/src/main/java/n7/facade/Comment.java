package n7.facade;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idCom;

    private String content;

    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    @JsonIgnore
    private Adherent auteur;

    @ManyToOne
    @JoinColumn(name = "recette_id", nullable = false)
    @JsonIgnore
    private Recette recette;

    public Comment() {
    }

    public Comment(int idCom, Adherent auteur, String content, Recette recette) {
        this.idCom = idCom;
        this.auteur = auteur;
        this.content = content;
        this.recette = recette;
    }

    public int getIdCom() {
        return idCom;
    }

    public void setIdCom(int idCom) {
        this.idCom = idCom;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Adherent getAuteur() {
        return auteur;
    }

    public void setAuteur(Adherent auteur) {
        this.auteur = auteur;
        if (!auteur.getComments().contains(this)) {
            auteur.getComments().add(this);
        }
    }

    public Recette getRecette() {
        return recette;
    }

    public void setRecette(Recette recette) {
        this.recette = recette;
        if (!recette.getComments().contains(this)) {
            recette.getComments().add(this);
        }
    }
}