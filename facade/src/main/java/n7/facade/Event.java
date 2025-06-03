package n7.facade;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String titre;
    private Date date;
    private String lieu;
    private String description;

    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    private Adherent auteur;

    @ManyToMany
    @JoinTable(
        name = "event_participants",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "adherent_id")
    )
    private List<Adherent> participants = new ArrayList<>();

    public Event() {
    }

    public Event(int id, String titre, Date date, String lieu, String description, Adherent auteur, List<Adherent> participants) {
        this.id = id;
        this.titre = titre;
        this.date = date;
        this.lieu = lieu;
        this.description = description;
        this.auteur = auteur;
        this.participants = participants;
    }

    // Getters et setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Adherent getAuteur() {
        return auteur;
    }

    public void setAuteur(Adherent auteur) {
        this.auteur = auteur;
    }

    public List<Adherent> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Adherent> participants) {
        this.participants = participants;
    }

  public void addParticipant(Adherent adherent) {
    if (!participants.contains(adherent)) {
        participants.add(adherent);
    }
}



}