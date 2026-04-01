package modele;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Commande {

    public enum Statut {
        PANIER("Panier"),
        PAYEE("Payée"),
        EN_PREPARATION("En préparation"),
        EXPEDIEE("Expédiée"),
        LIVREE("Livrée"),
        ANNULEE("Annulée");
        
        private final String libelle;
        
        Statut(String libelle) {
            this.libelle = libelle;
        }
        
        public String getLibelle() {
            return libelle;
        }
        
        @Override
        public String toString() {
            return libelle;
        }
    }
    
    // Propriétés JavaFX
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty clientId = new SimpleIntegerProperty();
    private final StringProperty clientNom = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> dateCommande = new SimpleObjectProperty<>();
    private final StringProperty dateCommandeFormatted = new SimpleStringProperty(); // Propriété pour la date formatée
    private final ObjectProperty<Statut> statut = new SimpleObjectProperty<>(Statut.PANIER);
    private final DoubleProperty total = new SimpleDoubleProperty();
    private final StringProperty adresseLivraison = new SimpleStringProperty();
    
    private List<DetailCommande> details = new ArrayList<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public Commande() {
        this.dateCommande.set(LocalDateTime.now());
        this.total.set(0.0);
        // Mettre à jour la date formatée quand la date change
        this.dateCommande.addListener((obs, old, newVal) -> {
            if (newVal != null) {
                dateCommandeFormatted.set(newVal.format(DATE_FORMATTER));
            }
        });
    }
    
    public Commande(int clientId, String clientNom, String adresseLivraison) {
        this();
        setClientId(clientId);
        setClientNom(clientNom);
        setAdresseLivraison(adresseLivraison);
    }
    
    // ============ GETTERS ET SETTERS ============
    
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }
    
    public int getClientId() { return clientId.get(); }
    public void setClientId(int value) { clientId.set(value); }
    public IntegerProperty clientIdProperty() { return clientId; }
    
    public String getClientNom() { return clientNom.get(); }
    public void setClientNom(String value) { clientNom.set(value); }
    public StringProperty clientNomProperty() { return clientNom; }
    
    public LocalDateTime getDateCommande() { return dateCommande.get(); }
    public void setDateCommande(LocalDateTime value) { 
        dateCommande.set(value);
        if (value != null) {
            dateCommandeFormatted.set(value.format(DATE_FORMATTER));
        }
    }
    public ObjectProperty<LocalDateTime> dateCommandeProperty() { return dateCommande; }
    
    // Propriété pour la date formatée (utilisée dans les tableaux)
    public String getDateCommandeFormatted() { 
        if (getDateCommande() != null) {
            return getDateCommande().format(DATE_FORMATTER);
        }
        return "";
    }
    public void setDateCommandeFormatted(String value) { dateCommandeFormatted.set(value); }
    public StringProperty dateCommandeFormattedProperty() { return dateCommandeFormatted; }
    
    public Statut getStatut() { return statut.get(); }
    public void setStatut(Statut value) { statut.set(value); }
    public ObjectProperty<Statut> statutProperty() { return statut; }
    
    public String getStatutLibelle() {
        return getStatut() != null ? getStatut().getLibelle() : "";
    }
    
    public double getTotal() { return total.get(); }
    public void setTotal(double value) { total.set(value); }
    public DoubleProperty totalProperty() { return total; }
    
    public String getAdresseLivraison() { return adresseLivraison.get(); }
    public void setAdresseLivraison(String value) { adresseLivraison.set(value); }
    public StringProperty adresseLivraisonProperty() { return adresseLivraison; }
    
    public List<DetailCommande> getDetails() { return details; }
    public void setDetails(List<DetailCommande> details) {
        this.details = details;
        calculerTotal();
    }
    
    // ============ MÉTHODES MÉTIERS ============
    
    public void ajouterProduit(Produit produit, int quantite) {
        if (!produit.hasEnoughStock(quantite)) {
            throw new IllegalStateException("Stock insuffisant");
        }
        
        for (DetailCommande detail : details) {
            if (detail.getProduitId() == produit.getId()) {
                detail.setQuantite(detail.getQuantite() + quantite);
                calculerTotal();
                produit.reserveStock(quantite);
                return;
            }
        }
        
        DetailCommande detail = new DetailCommande();
        detail.setProduitId(produit.getId());
        detail.setNomProduit(produit.getNom());
        detail.setQuantite(quantite);
        detail.setPrixUnitaire(produit.getPrix());
        
        details.add(detail);
        calculerTotal();
        produit.reserveStock(quantite);
    }
    
    public void retirerProduit(int index, Produit produit) {
        if (index >= 0 && index < details.size()) {
            DetailCommande detail = details.remove(index);
            produit.releaseStock(detail.getQuantite());
            calculerTotal();
        }
    }
    
    public void calculerTotal() {
        double nouveauTotal = details.stream()
                .mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite())
                .sum();
        setTotal(nouveauTotal);
    }
    
    public void valider(String adresseLivraison) {
        if (getStatut() != Statut.PANIER) {
            throw new IllegalStateException("La commande est déjà validée");
        }
        if (details.isEmpty()) {
            throw new IllegalStateException("Le panier est vide");
        }
        
        setAdresseLivraison(adresseLivraison);
        setStatut(Statut.PAYEE);
        setDateCommande(LocalDateTime.now());
    }
    
    public int getNombreArticles() {
        return details.stream().mapToInt(DetailCommande::getQuantite).sum();
    }
    
    public String toJson() {
        StringBuilder detailsJson = new StringBuilder("[");
        for (int i = 0; i < details.size(); i++) {
            detailsJson.append(details.get(i).toJson());
            if (i < details.size() - 1) detailsJson.append(",");
        }
        detailsJson.append("]");
        
        return String.format(
                "{\"id\":%d,\"clientId\":%d,\"clientNom\":\"%s\",\"dateCommande\":\"%s\",\"statut\":\"%s\",\"total\":%.2f,\"adresseLivraison\":\"%s\",\"details\":%s}",
                getId(), getClientId(), escapeJson(getClientNom()), getDateCommandeFormatted(),
                getStatut() != null ? getStatut().name() : "PANIER", getTotal(),
                escapeJson(getAdresseLivraison()), detailsJson
        );
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"");
    }
    
    @Override
    public String toString() {
        return String.format("Commande #%d - %s - %.2f€", getId(), getDateCommandeFormatted(), getTotal());
    }
}