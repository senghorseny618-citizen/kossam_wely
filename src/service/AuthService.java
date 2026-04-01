package service;

import dao.UtilisateurDAO;
import modele.Utilisateur;
import modele.Utilisateur.Role;

import java.util.Optional;

public class AuthService {

    private static AuthService instance;
    private final UtilisateurDAO utilisateurDAO;
    private Utilisateur currentUser;

    private AuthService() {
        this.utilisateurDAO = new UtilisateurDAO();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public Utilisateur login(String email, String password) {
        Optional<Utilisateur> user = utilisateurDAO.authenticate(email, password);
        if (user.isPresent()) {
            currentUser = user.get();
            System.out.println("✅ Session ouverte pour : " + currentUser.getNom());
            return currentUser;
        }
        return null;
    }

    public boolean register(String nom, String email, String password,
                            String telephone, String adresse, Role role) {
        Utilisateur newUser = new Utilisateur(nom, email, password, telephone, adresse, role);
        int id = utilisateurDAO.create(newUser);
        return id > 0;
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("👋 Session fermée pour : " + currentUser.getNom());
            currentUser = null;
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public Utilisateur getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    public boolean isProducteur() {
        return currentUser != null && currentUser.getRole() == Role.PRODUCTEUR;
    }

    public boolean isClient() {
        return currentUser != null && currentUser.getRole() == Role.CLIENT;
    }
}