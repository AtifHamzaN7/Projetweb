package n7.facade;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;




@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idMsg;

    private String content;

    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    @JsonIgnore
    private Adherent auteur;

    @ManyToOne
    @JsonIgnore
    @JsonBackReference
    @JoinColumn(name = "discussion_id", nullable = false)
    private Discussion discussion;

    public Message() {
    }

    public Message(int idMsg, Adherent auteur, String content, Discussion discussion) {
        this.idMsg = idMsg;
        this.auteur = auteur;
        this.content = content;
        this.discussion = discussion;
    }

    // Getters et setters
    public int getIdMsg() {
        return idMsg;
    }
    
    public void setIdMsg(int idMsg) {
        this.idMsg = idMsg;
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
        if (!auteur.getMessages().contains(this)) {
            auteur.getMessages().add(this);
        }
    }
    
    public Discussion getDiscussion() {
        return discussion;
    }
    
    public void setDiscussion(Discussion discussion) {
        this.discussion = discussion;
        if (!discussion.getMessages().contains(this)) {
            discussion.getMessages().add(this);
        }
    }
}