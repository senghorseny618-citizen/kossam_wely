package dao;

import modele.Utilisateur;
import modele.Utilisateur.Role;
import modele.Utilisateur.StatutValidation;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UtilisateurDAO {

    private Role mapRoleFromDatabase(String dbRole) {
        if (dbRole == null) return null;
        return switch (dbRole.toLowerCase()) {
            case "client" -> Role.CLIENT;
            case "producteur" -> Role.PRODUCTEUR;
            case "administrateur" -> Role.ADMIN;
            default -> Role.CLIENT;
        };
    }

    private String mapRoleToDatabase(Role role) {
        return switch (role) {
            case CLIENT -> "client";
            case PRODUCTEUR -> "producteur";
            case ADMIN -> "administrateur";
        };
    }
    
    private StatutValidation mapStatutFromDatabase(String dbStatut) {
        if (dbStatut == null) return StatutValidation.VALIDE;
        return switch (dbStatut.toUpperCase()) {
            case "EN_ATTENTE" -> StatutValidation.EN_ATTENTE;
            case "VALIDE" -> StatutValidation.VALIDE;
            case "REJETE" -> StatutValidation.REJETE;
            default -> StatutValidation.VALIDE;
        };
    }
    
    private String mapStatutToDatabase(StatutValidation statut) {
        return switch (statut) {
            case EN_ATTENTE -> "EN_ATTENTE";
            case VALIDE -> "VALIDE";
            case REJETE -> "REJETE";
        };
    }

    public int create(Utilisateur user) {
        String sql = "INSERT INTO utilisateurs (nom, email, mot_de_passe, telephone, adresse, role, is_active, statut_validation, date_demande) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        String hashedPassword = BCrypt.hashpw(user.getMotDePasse(), BCrypt.gensalt(12));

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, hashedPassword);
            stmt.setString(4, user.getTelephone());
            stmt.setString(5, user.getAdresse());
            stmt.setString(6, mapRoleToDatabase(user.getRole()));
            stmt.setBoolean(7, user.isActive());
            stmt.setString(8, mapStatutToDatabase(user.getStatutValidation()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur création utilisateur: " + e.getMessage());
        }
        return -1;
    }

    public Optional<Utilisateur> authenticate(String email, String password) {
        String sql = "SELECT * FROM utilisateurs WHERE email = ? AND is_active = true " +
                     "AND (role != 'producteur' OR statut_validation = 'VALIDE')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("mot_de_passe");
                if (BCrypt.checkpw(password, storedHash)) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur authentification: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Utilisateur> findAll() {
        List<Utilisateur> users = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs ORDER BY date_creation DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur liste utilisateurs: " + e.getMessage());
        }
        return users;
    }
    
    public List<Utilisateur> findProducteursEnAttente() {
        List<Utilisateur> users = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs WHERE role = 'producteur' AND statut_validation = 'EN_ATTENTE' ORDER BY date_demande ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findProducteursEnAttente: " + e.getMessage());
        }
        return users;
    }
    
    public boolean validerProducteur(int userId, boolean valider, String commentaire) {
        String sql = "UPDATE utilisateurs SET statut_validation = ?, date_validation = NOW(), commentaire_validation = ? WHERE id = ?";
        String statut = valider ? "VALIDE" : "REJETE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut);
            stmt.setString(2, commentaire);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur validerProducteur: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isProducteurValide(int userId) {
        String sql = "SELECT statut_validation FROM utilisateurs WHERE id = ? AND role = 'producteur'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return "VALIDE".equals(rs.getString("statut_validation"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur isProducteurValide: " + e.getMessage());
        }
        return false;
    }

    public Optional<Utilisateur> findById(int id) {
        String sql = "SELECT * FROM utilisateurs WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur recherche utilisateur: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean toggleActive(int userId, boolean active) {
        String sql = "UPDATE utilisateurs SET is_active = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, active);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur activation compte: " + e.getMessage());
            return false;
        }
    }

    private Utilisateur mapResultSetToUser(ResultSet rs) throws SQLException {
        Utilisateur user = new Utilisateur();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setEmail(rs.getString("email"));
        user.setMotDePasse("[PROTECTED]");
        user.setTelephone(rs.getString("telephone"));
        user.setAdresse(rs.getString("adresse"));
        user.setRole(mapRoleFromDatabase(rs.getString("role")));
        user.setActive(rs.getBoolean("is_active"));
        user.setStatutValidation(mapStatutFromDatabase(rs.getString("statut_validation")));
        return user;
    }
}