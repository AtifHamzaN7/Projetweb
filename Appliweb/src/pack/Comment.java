package pack;

public class Comment { 
    int idMsg;
    int idauteur;
    String content;
    int idRec;

    public Comment(int idMsg, int idauteur, String content, int idRec) {
        this.idMsg = idMsg;
        this.idauteur = idauteur;
        this.content = content;
        this.idRec = idRec;
    }

    public int getidMsg() {
        return idMsg;
    }

    public int getidauteur() {
        return idauteur;
    }

    public String getContent() {
        return content;
    }

    public int getidRec() {
        return idRec;
    }

    public void setidMsg(int idMsg) {
        this.idMsg = idMsg;
    }

    public void setidauteur(int idauteur) {
        this.idauteur = idauteur;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setidRec(int idRec) {
        this.idRec = idRec;
    }

    

}
