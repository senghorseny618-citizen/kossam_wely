package controller;

import application.MainApp;
import modele.Utilisateur.Role;
import service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField telephoneField;
    @FXML private TextArea adresseField;
    @FXML private RadioButton clientRadio;
    @FXML private RadioButton producteurRadio;
    @FXML private ToggleGroup roleGroup;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private VBox documentsContainer;
    @FXML private ListView<String> documentsListView;
    @FXML private Label documentsStatusLabel;

    private final AuthService authService;
    private final List<File> documentsFiles = new ArrayList<>();
    private static final String UPLOAD_DIR = "uploads/documents/";

    public RegisterController() {
        this.authService = AuthService.getInstance();
    }

    @FXML
    public void initialize() {
        // Validation en temps reel
        nomField.textProperty().addListener((obs, old, val) -> clearError());
        emailField.textProperty().addListener((obs, old, val) -> clearError());
        passwordField.textProperty().addListener((obs, old, val) -> clearError());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> clearError());
        telephoneField.textProperty().addListener((obs, old, val) -> clearError());
        adresseField.textProperty().addListener((obs, old, val) -> clearError());

        // Afficher le champ documents quand Producteur est selectionne
        producteurRadio.selectedProperty().addListener((obs, old, isSelected) -> {
            if (documentsContainer != null) {
                documentsContainer.setVisible(isSelected);
                documentsContainer.setManaged(isSelected);
            }
        });
        
        // Cacher le champ documents quand Client est selectionne
        clientRadio.selectedProperty().addListener((obs, old, isSelected) -> {
            if (documentsContainer != null && isSelected) {
                documentsContainer.setVisible(false);
                documentsContainer.setManaged(false);
                // Vider la liste si on change de role
                if (!documentsFiles.isEmpty()) {
                    documentsFiles.clear();
                    documentsListView.getItems().clear();
                    updateDocumentsStatus();
                }
            }
        });
        
        // Initialisation (Client par defaut, donc documents cache)
        if (documentsContainer != null) {
            documentsContainer.setVisible(false);
            documentsContainer.setManaged(false);
        }
        
        // Creer le dossier d'upload s'il n'existe pas
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            System.out.println("Dossier upload cree: " + Paths.get(UPLOAD_DIR).toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erreur creation dossier upload: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterDocument() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selectionner des documents justificatifs");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        
        Stage stage = (Stage) registerButton.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                // Verifier la taille du fichier (max 5 MB)
                if (file.length() > 5 * 1024 * 1024) {
                    showError("Le fichier " + file.getName() + " depasse 5 Mo");
                    continue;
                }
                documentsFiles.add(file);
                String fileInfo = file.getName() + " (" + formatFileSize(file.length()) + ")";
                documentsListView.getItems().add(fileInfo);
            }
            updateDocumentsStatus();
        }
    }
    
    @FXML
    private void supprimerDocument() {
        int selectedIndex = documentsListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            documentsFiles.remove(selectedIndex);
            documentsListView.getItems().remove(selectedIndex);
            updateDocumentsStatus();
        } else {
            showError("Veuillez selectionner un document a supprimer");
        }
    }
    
    @FXML
    private void viderListe() {
        documentsFiles.clear();
        documentsListView.getItems().clear();
        updateDocumentsStatus();
    }
    
    private void updateDocumentsStatus() {
        if (documentsFiles.isEmpty()) {
            documentsStatusLabel.setText("Aucun fichier selectionne");
            documentsStatusLabel.setStyle("-fx-text-fill: #dc3545;");
        } else {
            documentsStatusLabel.setText(documentsFiles.size() + " fichier(s) selectionne(s)");
            documentsStatusLabel.setStyle("-fx-text-fill: #28a745;");
        }
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
    
    private String saveDocuments(String email) throws IOException {
        if (documentsFiles.isEmpty()) return "";
        
        StringBuilder savedFiles = new StringBuilder();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userFolder = UPLOAD_DIR + email.replace("@", "_at_") + "_" + timestamp + "/";
        Files.createDirectories(Paths.get(userFolder));
        
        for (int i = 0; i < documentsFiles.size(); i++) {
            File sourceFile = documentsFiles.get(i);
            String originalName = sourceFile.getName();
            String extension = "";
            int lastDot = originalName.lastIndexOf(".");
            if (lastDot > 0) {
                extension = originalName.substring(lastDot);
            }
            String newFileName = "document_" + (i + 1) + "_" + System.currentTimeMillis() + extension;
            Path targetPath = Paths.get(userFolder + newFileName);
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            if (savedFiles.length() > 0) savedFiles.append(";");
            savedFiles.append(targetPath.toString());
        }
        
        return savedFiles.toString();
    }

    @FXML
    private void handleRegister() {
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String telephone = telephoneField.getText().trim();
        String adresse = adresseField.getText().trim();

        Role role = clientRadio.isSelected() ? Role.CLIENT : Role.PRODUCTEUR;

        // Validations
        if (nom.isEmpty() || email.isEmpty() || password.isEmpty() ||
                telephone.isEmpty() || adresse.isEmpty()) {
            showError("Tous les champs sont obligatoires");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Adresse email invalide");
            return;
        }

        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caracteres");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }
        
        // Verification des documents pour les producteurs
        if (role == Role.PRODUCTEUR && documentsFiles.isEmpty()) {
            showError("Veuillez importer des documents justificatifs (cliquez sur 'Ajouter des fichiers')");
            return;
        }

        try {
            String documentsPaths = "";
            if (role == Role.PRODUCTEUR) {
                documentsPaths = saveDocuments(email);
                System.out.println("Documents sauvegardes: " + documentsPaths);
            }
            
            boolean success = authService.register(nom, email, password, telephone, adresse, role, documentsPaths);

            if (success) {
                String message;
                if (role == Role.PRODUCTEUR) {
                    message = "VOTRE DEMANDE A ETE ENVOYEE A L'ADMINISTRATEUR\n\n" +
                              "Fichiers envoyes: " + documentsFiles.size() + " document(s)\n\n" +
                              "Les documents suivants ont ete telecharges:\n";
                    for (File f : documentsFiles) {
                        message += "  - " + f.getName() + "\n";
                    }
                    message += "\nVous serez notifie par email une fois votre compte valide.\n\n" +
                              "Le traitement peut prendre 24 a 48 heures.\n\n" +
                              "Merci de votre confiance !";
                } else {
                    message = "Votre compte a ete cree avec succes !\n\n" +
                              "Vous pouvez maintenant vous connecter.";
                }
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(role == Role.PRODUCTEUR ? "Demande envoyee" : "Inscription reussie");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
                goBackToLogin();
            } else {
                showError("Cette adresse email est deja utilisee");
            }

        } catch (Exception e) {
            showError("Erreur lors de l'inscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goBackToLogin() {
        MainApp.showLoginView();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}