package modele;

import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * Représente un produit laitier en vente
 */
public class Produit {

    // Propriétés JavaFX pour le binding
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final DoubleProperty prix = new SimpleDoubleProperty();
    private final IntegerProperty stock = new SimpleIntegerProperty();
    private final StringProperty unite = new SimpleStringProperty();
    private final IntegerProperty producteurId = new SimpleIntegerProperty();
    private final StringProperty producteurNom = new SimpleStringProperty(); // Ajout de la propriété producteurNom
    private final StringProperty imageUrl = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> dateCreation = new SimpleObjectProperty<>();
    private final BooleanProperty disponible = new SimpleBooleanProperty(true);
    
    // Constructeur par défaut
    public Produit() {
        this.dateCreation.set(LocalDateTime.now());
    }
    
    /**
     * Constructeur principal
     */
    public Produit(String nom, String description, double prix, int stock, String unite, int producteurId) {
        this();
        setNom(nom);
        setDescription(description);
        setPrix(prix);
        setStock(stock);
        setUnite(unite);
        setProducteurId(producteurId);
    }
    
    // ============ GETTERS ET SETTERS ============
    
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }
    
    public String getNom() { return nom.get(); }
    public void setNom(String value) { nom.set(value); }
    public StringProperty nomProperty() { return nom; }
    
    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
    public StringProperty descriptionProperty() { return description; }
    
    public double getPrix() { return prix.get(); }
    public void setPrix(double value) { prix.set(value); }
    public DoubleProperty prixProperty() { return prix; }
    
    public int getStock() { return stock.get(); }
    public void setStock(int value) { stock.set(value); }
    public IntegerProperty stockProperty() { return stock; }
    
    public String getUnite() { return unite.get(); }
    public void setUnite(String value) { unite.set(value); }
    public StringProperty uniteProperty() { return unite; }
    
    public int getProducteurId() { return producteurId.get(); }
    public void setProducteurId(int value) { producteurId.set(value); }
    public IntegerProperty producteurIdProperty() { return producteurId; }
    
    public String getProducteurNom() { return producteurNom.get(); }
    public void setProducteurNom(String value) { producteurNom.set(value); }
    public StringProperty producteurNomProperty() { return producteurNom; }
    
    public String getImageUrl() { return imageUrl.get(); }
    public void setImageUrl(String value) { imageUrl.set(value); }
    public StringProperty imageUrlProperty() { return imageUrl; }
    
    public LocalDateTime getDateCreation() { return dateCreation.get(); }
    public void setDateCreation(LocalDateTime value) { dateCreation.set(value); }
    public ObjectProperty<LocalDateTime> dateCreationProperty() { return dateCreation; }
    
    public boolean isDisponible() { return disponible.get(); }
    public void setDisponible(boolean value) { disponible.set(value); }
    public BooleanProperty disponibleProperty() { return disponible; }
    
    // ============ MÉTHODES MÉTIERS ============
    
    /**
     * Vérifie si le produit peut être commandé en quantité demandée
     */
    public boolean hasEnoughStock(int quantiteDemandee) {
        return isDisponible() && getStock() >= quantiteDemandee && quantiteDemandee > 0;
    }
    
    /**
     * Réserve du stock (diminue le stock disponible)
     */
    public boolean reserveStock(int quantite) {
        if (hasEnoughStock(quantite)) {
            setStock(getStock() - quantite);
            return true;
        }
        return false;
    }
    
    /**
     * Libère du stock (annulation de commande)
     */
    public void releaseStock(int quantite) {
        setStock(getStock() + quantite);
    }
    
    /**
     * Calcule le prix total pour une quantité
     */
    public double calculateTotal(int quantite) {
        return getPrix() * quantite;
    }
    
    /**
     * Conversion JSON pour JavaScript
     */
    public String toJson() {
        return String.format(
                "{\"id\":%d,\"nom\":\"%s\",\"description\":\"%s\",\"prix\":%.2f,\"stock\":%d,\"unite\":\"%s\",\"producteurId\":%d,\"producteurNom\":\"%s\",\"disponible\":%b,\"imageUrl\":\"%s\"}",
                getId(),
                escapeJson(getNom()),
                escapeJson(getDescription()),
                getPrix(),
                getStock(),
                escapeJson(getUnite()),
                getProducteurId(),
                escapeJson(getProducteurNom()),
                isDisponible(),
                escapeJson(getImageUrl())
        );
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
    
    @Override
    public String toString() {
        return String.format("Produit[%d] %s - %.2f€/%s (stock: %d)",
                getId(), getNom(), getPrix(), getUnite(), getStock());
    }
}