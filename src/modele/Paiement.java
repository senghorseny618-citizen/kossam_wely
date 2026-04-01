package modele;

import java.time.LocalDateTime;

/**
 * Historique des paiements
 *
 * Sépare la logique financière de la commande
 * Permet plusieurs tentatives de paiement si échec
 */
public class Paiement {

    public enum Methode {
        CARTE, PAYPAL, VIREMENT, ESPECES
    }

    public enum StatutPaiement {
        EN_ATTENTE,    // Paiement initié
        EFFECTUE,      // Paiement confirmé
        REFUSE,        // Carte refusée, solde insuffisant...
        REMBOURSE      // Remboursement effectué
    }

    private int id;
    private int commandeId;           // Commande associée
    private double montant;           // Montant payé (peut différer du total si remise)
    private Methode methode;          // Moyen de paiement
    private StatutPaiement statut;    // État du paiement
    private LocalDateTime datePaiement;
    private String transactionId;     // ID externe (Stripe, PayPal...)
    private String commentaire;       // Raison du refus, note...

    // Constructeur
    public Paiement() {
        this.datePaiement = LocalDateTime.now();
        this.statut = StatutPaiement.EN_ATTENTE;
    }

    public Paiement(int commandeId, double montant, Methode methode) {
        this();
        this.commandeId = commandeId;
        this.montant = montant;
        this.methode = methode;
    }

    // ============ GETTERS & SETTERS ============

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public Methode getMethode() { return methode; }
    public void setMethode(Methode methode) { this.methode = methode; }

    public StatutPaiement getStatut() { return statut; }
    public void setStatut(StatutPaiement statut) { this.statut = statut; }

    public LocalDateTime getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDateTime datePaiement) { this.datePaiement = datePaiement; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    // ============ MÉTHODES MÉTIERS ============

    /**
     * Marque le paiement comme effectué
     */
    public void confirmer(String transactionId) {
        this.transactionId = transactionId;
        this.statut = StatutPaiement.EFFECTUE;
    }

    /**
     * Marque le paiement comme refusé
     */
    public void refuser(String raison) {
        this.statut = StatutPaiement.REFUSE;
        this.commentaire = raison;
    }

    /**
     * Conversion JSON
     */
    public String toJson() {
        return String.format(
                "{\"id\":%d,\"commandeId\":%d,\"montant\":%.2f,\"methode\":\"%s\",\"statut\":\"%s\",\"datePaiement\":\"%s\",\"transactionId\":\"%s\"}",
                id, commandeId, montant, methode, statut, datePaiement,
                escapeJson(transactionId)
        );
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"");
    }
}