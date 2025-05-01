package n7.facade;

import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;

@Entity
public class Discussion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idDisc;

    private String titre;
    private String question;

    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    private Adherent auteur;

    @OneToMany(mappedBy = "discussion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();

    public Discussion() {
    }

    public Discussion(int idDisc, String titre, String question, Adherent auteur) {
        this.idDisc = idDisc;
        this.titre = titre;
        this.question = question;
        this.auteur = auteur;
    }
    // Getters et setters
    public int getIdDisc() {
        return idDisc;
    }
    
    public void setIdDisc(int idDisc) {
        this.idDisc = idDisc;
    }
    
    public String getTitre() {
        return titre;
    }
    
    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public Adherent getAuteur() {
        return auteur;
    }
    
    public void setAuteur(Adherent auteur) {
        this.auteur = auteur;
        if (!auteur.getDiscussions().contains(this)) {
            auteur.getDiscussions().add(this);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}