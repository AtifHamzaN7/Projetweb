package pack;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class Event {
    int id;
    String titre;
    Date date;
    String lieu;
    String description;
    String auteur;
    List<Adherent> participants;


    public Event(int id, String titre, Date date, String lieu, String description, String auteur, List<Adherent> participants) {
        this.id = id;
        this.titre = titre;
        this.date = date;
        this.lieu = lieu;
        this.description = description;
        this.auteur = auteur;
        this.participants = participants;
    }

    public int getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public Date getDate() {
        return date;
    }

    public String getLieu() {
        return lieu;
    }

    public String getDescription() {
        return description;
    }

    public String getAuteur() {
        return auteur;
    }

    public List<Adherent> getParticipants() {
        return participants;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public void setParticipants(List<Adherent> participants) {
        this.participants = participants;
    }

    public void addParticipant(Adherent adherent) {
    if (participants == null) {
        participants = new ArrayList<>();
    }
    participants.add(adherent);
    }
    
}
