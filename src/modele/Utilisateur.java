package modele;

public class Utilisateur {

    public enum Role {
        CLIENT, PRODUCTEUR, ADMIN
    }
    
    public enum StatutValidation {
        EN_ATTENTE, VALIDE, REJETE
    }

    private int id;
    private String nom;
    private String email;
    private String motDePasse;
    private String telephone;
    private String adresse;
    private Role role;
    private boolean isActive;
    private StatutValidation statutValidation;
    private String dateDemande;
    private String dateValidation;
    private String commentaireValidation;
    private String documentsJustificatifs;

    public Utilisateur() {}

    public Utilisateur(String nom, String email, String motDePasse,
                       String telephone, String adresse, Role role) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.telephone = telephone;
        this.adresse = adresse;
        this.role = role;
        this.isActive = true;
        this.statutValidation = (role == Role.PRODUCTEUR) ? StatutValidation.EN_ATTENTE : StatutValidation.VALIDE;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public StatutValidation getStatutValidation() { return statutValidation; }
    public void setStatutValidation(StatutValidation statutValidation) { this.statutValidation = statutValidation; }
    public String getDateDemande() { return dateDemande; }
    public void setDateDemande(String dateDemande) { this.dateDemande = dateDemande; }
    public String getDateValidation() { return dateValidation; }
    public void setDateValidation(String dateValidation) { this.dateValidation = dateValidation; }
    public String getCommentaireValidation() { return commentaireValidation; }
    public void setCommentaireValidation(String commentaireValidation) { this.commentaireValidation = commentaireValidation; }
    public String getDocumentsJustificatifs() { return documentsJustificatifs; }
    public void setDocumentsJustificatifs(String documentsJustificatifs) { this.documentsJustificatifs = documentsJustificatifs; }

    public String toJson() {
        return String.format(
                "{\"id\":%d,\"nom\":\"%s\",\"email\":\"%s\",\"telephone\":\"%s\",\"adresse\":\"%s\",\"role\":\"%s\",\"isActive\":%b,\"statutValidation\":\"%s\"}",
                id, escapeJson(nom), escapeJson(email), escapeJson(telephone), 
                escapeJson(adresse), role, isActive, statutValidation != null ? statutValidation.name() : "VALIDE"
        );
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}