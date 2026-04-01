package dao;

import modele.Produit;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitDAO {

    public int create(Produit produit) {
        String sql = "INSERT INTO produits (nom, description, prix, stock, unite, producteur_id, disponible, image_url, date_creation) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setDouble(3, produit.getPrix());
            stmt.setInt(4, produit.getStock());
            stmt.setString(5, produit.getUnite());
            stmt.setInt(6, produit.getProducteurId());
            stmt.setBoolean(7, produit.isDisponible());
            stmt.setString(8, produit.getImageUrl());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur création produit: " + e.getMessage());
        }
        return -1;
    }

    public List<Produit> findAllAvailable() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, u.nom as producteur_nom FROM produits p " +
                     "JOIN utilisateurs u ON p.producteur_id = u.id " +
                     "WHERE p.disponible = true AND p.stock > 0 " +
                     "ORDER BY p.date_creation DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Produit p = mapResultSetToProduit(rs);
                p.setProducteurNom(rs.getString("producteur_nom"));
                produits.add(p);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findAllAvailable: " + e.getMessage());
        }
        return produits;
    }

    public List<Produit> findByProducteur(int producteurId) {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produits WHERE producteur_id = ? ORDER BY date_creation DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, producteurId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByProducteur: " + e.getMessage());
        }
        return produits;
    }

    public Produit findById(int id) {
        String sql = "SELECT p.*, u.nom as producteur_nom FROM produits p " +
                     "JOIN utilisateurs u ON p.producteur_id = u.id " +
                     "WHERE p.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Produit p = mapResultSetToProduit(rs);
                p.setProducteurNom(rs.getString("producteur_nom"));
                return p;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById: " + e.getMessage());
        }
        return null;
    }

    public boolean update(Produit produit) {
        String sql = "UPDATE produits SET nom = ?, description = ?, prix = ?, stock = ?, unite = ?, disponible = ?, image_url = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setDouble(3, produit.getPrix());
            stmt.setInt(4, produit.getStock());
            stmt.setString(5, produit.getUnite());
            stmt.setBoolean(6, produit.isDisponible());
            stmt.setString(7, produit.getImageUrl());
            stmt.setInt(8, produit.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur update produit: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int produitId) {
        String sql = "UPDATE produits SET disponible = false WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, produitId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur delete produit: " + e.getMessage());
            return false;
        }
    }

    public boolean toggleDisponible(int produitId, boolean disponible) {
        String sql = "UPDATE produits SET disponible = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, disponible);
            stmt.setInt(2, produitId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur toggleDisponible: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStock(int produitId, int newStock) {
        String sql = "UPDATE produits SET stock = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newStock);
            stmt.setInt(2, produitId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateStock: " + e.getMessage());
            return false;
        }
    }

    public int countByProducteur(int producteurId) {
        String sql = "SELECT COUNT(*) FROM produits WHERE producteur_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, producteurId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur countByProducteur: " + e.getMessage());
        }
        return 0;
    }

    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setPrix(rs.getDouble("prix"));
        p.setStock(rs.getInt("stock"));
        p.setUnite(rs.getString("unite"));
        p.setProducteurId(rs.getInt("producteur_id"));
        p.setDisponible(rs.getBoolean("disponible"));
        p.setImageUrl(rs.getString("image_url"));
        if (rs.getTimestamp("date_creation") != null) {
            p.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
        }
        return p;
    }
}