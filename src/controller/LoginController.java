package controller;

import application.MainApp;
import modele.Utilisateur;
import service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label errorLabel;

    private final AuthService authService;

    public LoginController() {
        this.authService = AuthService.getInstance();
    }

    @FXML
    public void initialize() {
        emailField.textProperty().addListener((obs, old, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, old, newVal) -> hideError());
        passwordField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        Utilisateur user = authService.login(email, password);

        if (user != null) {
            System.out.println("✅ Connexion réussie : " + user.getNom());
            MainApp.showDashboard(user.getRole().name());
        } else {
            showError("Email ou mot de passe incorrect");
        }
    }

    @FXML
    private void forgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mot de passe oublié");
        alert.setHeaderText("Réinitialisation du mot de passe");
        alert.setContentText("Un email de réinitialisation vous sera envoyé prochainement.");
        alert.showAndWait();
    }

    @FXML
    private void googleLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Connexion Google");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("La connexion avec Google sera bientôt disponible.");
        alert.showAndWait();
    }

    @FXML
    private void facebookLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Connexion Facebook");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("La connexion avec Facebook sera bientôt disponible.");
        alert.showAndWait();
    }

    @FXML
    private void githubLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("GitHub Login");
        alert.setHeaderText("Coming Soon");
        alert.setContentText("GitHub authentication will be available soon!");
        alert.showAndWait();
    }

    @FXML
    private void appleLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Connexion Apple");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("La connexion avec Apple sera bientôt disponible.");
        alert.showAndWait();
    }

    @FXML
    private void goToRegister() {
        MainApp.showRegisterView();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}