package modele;

/**
 * Représente un utilisateur du système (Client, Producteur, ou Admin)
 *
 * Concepts importants :
 * - ENUM Role : type sûr, valeurs limitées (impossible de mettre "clien" au lieu de "CLIENT")
 * - Encapsulation : tous les champs private, accès via getters/setters
 * - isActive : permet de désactiver un compte sans le supprimer (soft delete)
 */
public class Utilisateur {

    /**
     * Enumération des rôles possibles
     * CLIENT    : peut acheter des produits
     * PRODUCTEUR : peut vendre des produits
     * ADMIN     : gère la plateforme
     */
    public enum Role {
        CLIENT, PRODUCTEUR, ADMIN
    }

    // Attributs privés (encapsulation - protection des données)
    private int id;
    private String nom;
    private String email;
    private String motDePasse;   // Stocké hashé avec BCrypt ! Jamais en clair.
    private String telephone;      // Pour contacter l'utilisateur
    private String adresse;        // Pour la livraison
    private Role role;
    private boolean isActive;      // Compte actif ou désactivé

    // Constructeur vide (nécessaire pour certaines librairies comme Hibernate ou JSON)
    public Utilisateur() {}

    /**
     * Constructeur complet (sauf ID qui est auto-généré par MySQL)
     */
    public Utilisateur(String nom, String email, String motDePasse,
                       String telephone, String adresse, Role role) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.telephone = telephone;
        this.adresse = adresse;
        this.role = role;
        this.isActive = true; // Par défaut, compte actif à la création
    }

    // ============ GETTERS & SETTERS ============

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // ============ MÉTHODES MÉTIERS ============

    /**
     * Convertit l'utilisateur en JSON (pour communication avec JavaScript)
     *  EXCLUT le mot de passe pour la sécurité !
     */
    public String toJson() {
        return String.format(
                "{\"id\":%d,\"nom\":\"%s\",\"email\":\"%s\",\"telephone\":\"%s\",\"adresse\":\"%s\",\"role\":\"%s\",\"isActive\":%b}",
                id,
                escapeJson(nom),
                escapeJson(email),
                escapeJson(telephone),
                escapeJson(adresse),
                role,
                isActive
        );
    }

    /**
     * Échappe les caractères spéciaux JSON pour éviter les injections
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                '}';
    }
}