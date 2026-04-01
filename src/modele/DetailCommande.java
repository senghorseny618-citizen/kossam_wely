package modele;

/**
 * Ligne de commande (un produit dans une commande)
 *
 * IMPORTANT : On duplique nomProduit et prixUnitaire car :
 * - Le produit original peut changer de prix plus tard
 * - Le produit peut être supprimé
 * - L'historique de commande doit rester fidèle au moment de l'achat
 */
public class DetailCommande {

    private int id;
    private int commandeId;        // Clé étrangère vers Commande
    private int produitId;         // Clé étrangère vers Produit (référence)
    private String nomProduit;     // COPIE du nom au moment de l'achat
    private int quantite;
    private double prixUnitaire;   // COPIE du prix au moment de l'achat

    // Constructeur vide
    public DetailCommande() {}

    // ============ GETTERS & SETTERS ============

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    // ============ MÉTHODES MÉTIERS ============

    /**
     * Calcule le sous-total de cette ligne
     */
    public double getSousTotal() {
        return prixUnitaire * quantite;
    }

    /**
     * Conversion JSON
     */
    public String toJson() {
        return String.format(
                "{\"id\":%d,\"produitId\":%d,\"nomProduit\":\"%s\",\"quantite\":%d,\"prixUnitaire\":%.2f,\"sousTotal\":%.2f}",
                id, produitId, escapeJson(nomProduit), quantite, prixUnitaire, getSousTotal()
        );
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"");
    }

    @Override
    public String toString() {
        return String.format("%s x%d @ %.2f€ = %.2f€",
                nomProduit, quantite, prixUnitaire, getSousTotal());
    }
}