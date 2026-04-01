package controller;

import application.MainApp;
import modele.Utilisateur.Role;
import service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    private final AuthService authService;

    public RegisterController() {
        this.authService = AuthService.getInstance();
    }

    @FXML
    public void initialize() {
        nomField.textProperty().addListener((obs, old, val) -> clearError());
        emailField.textProperty().addListener((obs, old, val) -> clearError());
        passwordField.textProperty().addListener((obs, old, val) -> clearError());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> clearError());
        telephoneField.textProperty().addListener((obs, old, val) -> clearError());
        adresseField.textProperty().addListener((obs, old, val) -> clearError());

        adresseField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                handleRegister();
            }
        });
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

        if (nom.isEmpty() || email.isEmpty() || password.isEmpty() ||
                telephone.isEmpty() || adresse.isEmpty()) {
            showError("Tous les champs sont obligatoires");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("L'adresse email n'est pas valide");
            return;
        }

        if (!telephone.matches("^[0-9\\s\\+\\-\\(\\)]{8,20}$")) {
            showError("Le numéro de téléphone n'est pas valide");
            return;
        }

        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            boolean success = authService.register(nom, email, password, telephone, adresse, role);

            if (success) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Inscription réussie");
                alert.setHeaderText(null);
                alert.setContentText("Votre compte a été créé avec succès ! Vous pouvez maintenant vous connecter.");
                alert.showAndWait();
                goBackToLogin();
            } else {
                showError("Cette adresse email est déjà utilisée");
            }

        } catch (Exception e) {
            showError("Erreur lors de l'inscription : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goBackToLogin() {
        MainApp.showLoginView();
    }

    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}