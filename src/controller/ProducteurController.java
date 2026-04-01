package controller;

import application.MainApp;
import modele.Commande;
import modele.Produit;
import service.AuthService;
import service.ProduitService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;

public class ProducteurController {

    @FXML private Label statsProduits;
    @FXML private Label statsCommandes;
    @FXML private Label statsCA;
    
    @FXML private VBox produitsSection;
    @FXML private VBox commandesSection;
    @FXML private VBox ajouterSection;
    
    @FXML private TextField searchField;
    @FXML private TableView<Produit> produitsTable;
    
    @FXML private TableView<Commande> commandesTable;
    
    @FXML private TextField produitNom;
    @FXML private TextArea produitDescription;
    @FXML private TextField produitPrix;
    @FXML private TextField produitStock;
    @FXML private ComboBox<String> produitUnite;
    @FXML private TextField produitImage;
    
    @FXML private Button tabProduitsBtn;
    @FXML private Button tabCommandesBtn;
    @FXML private Button tabAjouterBtn;
    
    private final AuthService authService = AuthService.getInstance();
    private final ProduitService produitService = new ProduitService();
    
    private ObservableList<Produit> produitsList = FXCollections.observableArrayList();
    private ObservableList<Commande> commandesList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        // Initialiser la combo unité
        produitUnite.setItems(FXCollections.observableArrayList("litre", "kg", "unité", "bouteille", "pot"));
        produitUnite.setValue("unité");
        
        // Charger les données
        chargerStatistiques();
        chargerProduits();
        chargerCommandes();
        
        // Configurer les tableaux
        configurerTableProduits();
        configurerTableCommandes();
        
        // Recherche en temps réel
        searchField.textProperty().addListener((obs, old, val) -> filtrerProduits(val));
    }
    
    private void chargerStatistiques() {
        int nbProduits = produitService.getNombreProduitsByProducteur(authService.getCurrentUser().getId());
        int nbCommandes = produitService.getNombreCommandesByProducteur(authService.getCurrentUser().getId());
        double ca = produitService.getCA(authService.getCurrentUser().getId());
        
        statsProduits.setText(String.valueOf(nbProduits));
        statsCommandes.setText(String.valueOf(nbCommandes));
        statsCA.setText(String.format("%,d FCFA", (int) ca));
    }
    
    private void chargerProduits() {
        List<Produit> produits = produitService.getProduitsByProducteur(authService.getCurrentUser().getId());
        produitsList.setAll(produits);
        produitsTable.setItems(produitsList);
    }
    
    private void chargerCommandes() {
        List<Commande> commandes = produitService.getCommandesByProducteur(authService.getCurrentUser().getId());
        commandesList.setAll(commandes);
        commandesTable.setItems(commandesList);
    }
    
    @SuppressWarnings("unchecked")
    private void configurerTableProduits() {
        // Configuration des colonnes
        TableColumn<Produit, Number> colId = (TableColumn<Produit, Number>) produitsTable.getColumns().get(0);
        TableColumn<Produit, String> colNom = (TableColumn<Produit, String>) produitsTable.getColumns().get(1);
        TableColumn<Produit, Number> colPrix = (TableColumn<Produit, Number>) produitsTable.getColumns().get(2);
        TableColumn<Produit, Number> colStock = (TableColumn<Produit, Number>) produitsTable.getColumns().get(3);
        TableColumn<Produit, String> colUnite = (TableColumn<Produit, String>) produitsTable.getColumns().get(4);
        TableColumn<Produit, Boolean> colStatut = (TableColumn<Produit, Boolean>) produitsTable.getColumns().get(5);
        
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colNom.setCellValueFactory(cellData -> cellData.getValue().nomProperty());
        colPrix.setCellValueFactory(cellData -> cellData.getValue().prixProperty());
        colStock.setCellValueFactory(cellData -> cellData.getValue().stockProperty());
        colUnite.setCellValueFactory(cellData -> cellData.getValue().uniteProperty());
        colStatut.setCellValueFactory(cellData -> cellData.getValue().disponibleProperty());
        
        // Formater le prix
        colPrix.setCellFactory(column -> new TableCell<Produit, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d FCFA", item.intValue()));
                }
            }
        });
        
        // Formater le statut
        colStatut.setCellFactory(column -> new TableCell<Produit, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "Disponible" : "Indisponible");
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });
        
        // Colonne Actions
        TableColumn<Produit, Void> colActions = (TableColumn<Produit, Void>) produitsTable.getColumns().get(6);
        colActions.setCellFactory(param -> new TableCell<Produit, Void>() {
            private final Button btnToggle = new Button();
            private final Button btnDelete = new Button();
            private final HBox buttons = new HBox(5, btnToggle, btnDelete);
            
            {
                btnToggle.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-font-size: 10; -fx-padding: 5 10; -fx-background-radius: 15; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 5 10; -fx-background-radius: 15; -fx-cursor: hand;");
                
                btnToggle.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    produitService.toggleDisponibilite(produit.getId());
                    chargerProduits();
                });
                
                btnDelete.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    if (confirmDialog("Supprimer le produit", "Voulez-vous vraiment supprimer ce produit ?")) {
                        produitService.supprimerProduit(produit.getId());
                        chargerProduits();
                        chargerStatistiques();
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Produit produit = getTableView().getItems().get(getIndex());
                    btnToggle.setText(produit.isDisponible() ? "Désactiver" : "Activer");
                    setGraphic(buttons);
                }
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private void configurerTableCommandes() {
        TableColumn<Commande, Number> colId = (TableColumn<Commande, Number>) commandesTable.getColumns().get(0);
        TableColumn<Commande, String> colDate = (TableColumn<Commande, String>) commandesTable.getColumns().get(1);
        TableColumn<Commande, String> colClient = (TableColumn<Commande, String>) commandesTable.getColumns().get(2);
        TableColumn<Commande, Number> colTotal = (TableColumn<Commande, Number>) commandesTable.getColumns().get(3);
        TableColumn<Commande, String> colStatut = (TableColumn<Commande, String>) commandesTable.getColumns().get(4);
        
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateCommandeFormattedProperty()); // Utiliser la propriété formatée
        colClient.setCellValueFactory(cellData -> cellData.getValue().clientNomProperty());
        colTotal.setCellValueFactory(cellData -> cellData.getValue().totalProperty());
        colStatut.setCellValueFactory(cellData -> {
            // Convertir ObjectProperty<Statut> en ObservableValue<String>
            return new SimpleStringProperty(cellData.getValue().getStatutLibelle());
        });
        
        // Formater le total
        colTotal.setCellFactory(column -> new TableCell<Commande, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d FCFA", item.intValue()));
                }
            }
        });
        
        // Colonne Actions
        TableColumn<Commande, Void> colActions = (TableColumn<Commande, Void>) commandesTable.getColumns().get(5);
        colActions.setCellFactory(param -> new TableCell<Commande, Void>() {
            private final ComboBox<String> statutCombo = new ComboBox<>();
            
            {
                statutCombo.setItems(FXCollections.observableArrayList("PAYEE", "EN_PREPARATION", "EXPEDIEE", "LIVREE"));
                statutCombo.setStyle("-fx-background-color: #f8f9fa; -fx-font-size: 11; -fx-padding: 5;");
                statutCombo.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    String nouveauStatut = statutCombo.getValue();
                    if (nouveauStatut != null) {
                        produitService.mettreAJourStatutCommande(commande.getId(), nouveauStatut);
                        chargerCommandes();
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commande commande = getTableView().getItems().get(getIndex());
                    statutCombo.setValue(commande.getStatut().name());
                    setGraphic(statutCombo);
                }
            }
        });
    }
    
    @FXML
    private void showProduits() {
        produitsSection.setVisible(true);
        produitsSection.setManaged(true);
        commandesSection.setVisible(false);
        commandesSection.setManaged(false);
        ajouterSection.setVisible(false);
        ajouterSection.setManaged(false);
        
        tabProduitsBtn.setStyle("-fx-background-color: #2c5f2d; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 10 25;");
        tabCommandesBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #2c5f2d; -fx-background-radius: 30; -fx-padding: 10 25;");
        tabAjouterBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #2c5f2d; -fx-background-radius: 30; -fx-padding: 10 25;");
    }
    
    @FXML
    private void showCommandes() {
        produitsSection.setVisible(false);
        produitsSection.setManaged(false);
        commandesSection.setVisible(true);
        commandesSection.setManaged(true);
        ajouterSection.setVisible(false);
        ajouterSection.setManaged(false);
        
        tabProduitsBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #2c5f2d; -fx-background-radius: 30; -fx-padding: 10 25;");
        tabCommandesBtn.setStyle("-fx-background-color: #2c5f2d; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 10 25;");
        tabAjouterBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #2c5f2d; -fx-background-radius: 30; -fx-padding: 10 25;");
        
        chargerCommandes();
    }
    
    @FXML
    private void showAjouterProduit() {
        produitsSection.setVisible(false);
        produitsSection.setManaged(false);
        commandesSection.setVisible(false);
        commandesSection.setManaged(false);
        ajouterSection.setVisible(true);
        ajouterSection.setManaged(true);
        
        tabProduitsBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #2c5f2d; -fx-background-radius: 30; -fx-padding: 10 25;");
        tabCommandesBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #2c5f2d; -fx-background-radius: 30; -fx-padding: 10 25;");
        tabAjouterBtn.setStyle("-fx-background-color: #2c5f2d; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 10 25;");
    }
    
    @FXML
    private void ajouterProduit() {
        if (produitNom.getText().isEmpty() || produitPrix.getText().isEmpty() || produitStock.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires");
            return;
        }
        
        try {
            Produit produit = new Produit();
            produit.setNom(produitNom.getText());
            produit.setDescription(produitDescription.getText());
            produit.setPrix(Double.parseDouble(produitPrix.getText()));
            produit.setStock(Integer.parseInt(produitStock.getText()));
            produit.setUnite(produitUnite.getValue());
            produit.setProducteurId(authService.getCurrentUser().getId());
            produit.setImageUrl(produitImage.getText());
            produit.setDisponible(true);
            
            produitService.ajouterProduit(produit);
            
            showAlert("Succès", "Produit ajouté avec succès !");
            resetFormulaire();
            chargerProduits();
            chargerStatistiques();
            showProduits();
            
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez saisir des nombres valides pour le prix et le stock");
        }
    }
    
    @FXML
    private void resetFormulaire() {
        produitNom.clear();
        produitDescription.clear();
        produitPrix.clear();
        produitStock.clear();
        produitImage.clear();
        produitUnite.setValue("unité");
    }
    
    private void filtrerProduits(String recherche) {
        if (recherche == null || recherche.isEmpty()) {
            produitsTable.setItems(produitsList);
        } else {
            ObservableList<Produit> filtres = produitsList.filtered(p -> 
                p.getNom().toLowerCase().contains(recherche.toLowerCase()) ||
                p.getDescription().toLowerCase().contains(recherche.toLowerCase())
            );
            produitsTable.setItems(filtres);
        }
    }
    
    @FXML
    private void logout() {
        authService.logout();
        MainApp.showLoginView();
    }
    
    private boolean confirmDialog(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
    
    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}