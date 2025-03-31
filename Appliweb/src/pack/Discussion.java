package pack;

public class Discussion {

    int idDisc;
    String titre;
    String question;
    int idauteur;

    public Discussion(int idDisc, String titre, String question, int idauteur) {
        this.idDisc = idDisc;
        this.titre = titre;
        this.question = question;
        this.idauteur = idauteur;
    }

    public int getidDisc() {
        return idDisc;
    }

    public String getTitre() {
        return titre;
    }

    public String getQuestion() {
        return question;
    }


    public int getidauteur() {
        return idauteur;
    }

    public void setidDisc(int idDisc) {
        this.idDisc = idDisc;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    


}
