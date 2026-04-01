package dao;

import modele.Commande;
import modele.DetailCommande;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeDAO {

    public List<Commande> findByProducteur(int producteurId) {
        List<Commande> commandes = new ArrayList<>();
        String sql = "SELECT DISTINCT c.*, u.nom as client_nom FROM commandes c " +
                     "JOIN details_commande d ON c.id = d.commande_id " +
                     "JOIN produits p ON d.produit_id = p.id " +
                     "JOIN utilisateurs u ON c.client_id = u.id " +
                     "WHERE p.producteur_id = ? ORDER BY c.date_commande DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, producteurId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Commande c = mapResultSetToCommande(rs);
                c.setClientNom(rs.getString("client_nom"));
                loadDetails(c);
                commandes.add(c);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByProducteur: " + e.getMessage());
        }
        return commandes;
    }

    public List<Commande> findByClient(int clientId) {
        List<Commande> commandes = new ArrayList<>();
        String sql = "SELECT * FROM commandes WHERE client_id = ? ORDER BY date_commande DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Commande c = mapResultSetToCommande(rs);
                loadDetails(c);
                commandes.add(c);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByClient: " + e.getMessage());
        }
        return commandes;
    }

    public boolean updateStatus(int commandeId, String statut) {
        String sql = "UPDATE commandes SET statut = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut.toUpperCase());
            stmt.setInt(2, commandeId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateStatus: " + e.getMessage());
            return false;
        }
    }

    public int countByProducteur(int producteurId) {
        String sql = "SELECT COUNT(DISTINCT c.id) FROM commandes c " +
                     "JOIN details_commande d ON c.id = d.commande_id " +
                     "JOIN produits p ON d.produit_id = p.id " +
                     "WHERE p.producteur_id = ?";

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

    public double getCA(int producteurId) {
        String sql = "SELECT SUM(d.quantite * d.prix_unitaire) as ca FROM commandes c " +
                     "JOIN details_commande d ON c.id = d.commande_id " +
                     "JOIN produits p ON d.produit_id = p.id " +
                     "WHERE p.producteur_id = ? AND c.statut IN ('PAYEE', 'EN_PREPARATION', 'EXPEDIEE', 'LIVREE')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, producteurId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("ca");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getCA: " + e.getMessage());
        }
        return 0;
    }

    public List<ProduitVente> getTopVentes(int producteurId, int limit) {
        List<ProduitVente> topVentes = new ArrayList<>();
        String sql = "SELECT p.id, p.nom, p.unite, SUM(d.quantite) as total_vendu, SUM(d.quantite * d.prix_unitaire) as total_ca " +
                     "FROM produits p " +
                     "JOIN details_commande d ON p.id = d.produit_id " +
                     "JOIN commandes c ON d.commande_id = c.id " +
                     "WHERE p.producteur_id = ? AND c.statut IN ('PAYEE', 'EN_PREPARATION', 'EXPEDIEE', 'LIVREE') " +
                     "GROUP BY p.id, p.nom, p.unite " +
                     "ORDER BY total_vendu DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, producteurId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProduitVente pv = new ProduitVente();
                pv.setId(rs.getInt("id"));
                pv.setNom(rs.getString("nom"));
                pv.setUnite(rs.getString("unite"));
                pv.setVendu(rs.getInt("total_vendu"));
                pv.setTotal(rs.getDouble("total_ca"));
                topVentes.add(pv);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTopVentes: " + e.getMessage());
        }
        return topVentes;
    }

    public int create(Commande commande) {
        String sql = "INSERT INTO commandes (client_id, date_commande, statut, total, adresse_livraison) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, commande.getClientId());
            stmt.setTimestamp(2, Timestamp.valueOf(commande.getDateCommande()));
            stmt.setString(3, commande.getStatut().name());
            stmt.setDouble(4, commande.getTotal());
            stmt.setString(5, commande.getAdresseLivraison());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int commandeId = rs.getInt(1);
                        createDetails(commandeId, commande.getDetails());
                        return commandeId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur create commande: " + e.getMessage());
        }
        return -1;
    }

    private void createDetails(int commandeId, List<DetailCommande> details) {
        String sql = "INSERT INTO details_commande (commande_id, produit_id, nom_produit, quantite, prix_unitaire) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (DetailCommande detail : details) {
                stmt.setInt(1, commandeId);
                stmt.setInt(2, detail.getProduitId());
                stmt.setString(3, detail.getNomProduit());
                stmt.setInt(4, detail.getQuantite());
                stmt.setDouble(5, detail.getPrixUnitaire());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("❌ Erreur createDetails: " + e.getMessage());
        }
    }

    private void loadDetails(Commande commande) {
        String sql = "SELECT * FROM details_commande WHERE commande_id = ?";
        List<DetailCommande> details = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, commande.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                DetailCommande d = new DetailCommande();
                d.setId(rs.getInt("id"));
                d.setCommandeId(rs.getInt("commande_id"));
                d.setProduitId(rs.getInt("produit_id"));
                d.setNomProduit(rs.getString("nom_produit"));
                d.setQuantite(rs.getInt("quantite"));
                d.setPrixUnitaire(rs.getDouble("prix_unitaire"));
                details.add(d);
            }
            commande.setDetails(details);
            commande.calculerTotal();
        } catch (SQLException e) {
            System.err.println("❌ Erreur loadDetails: " + e.getMessage());
        }
    }

    private Commande mapResultSetToCommande(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId(rs.getInt("id"));
        c.setClientId(rs.getInt("client_id"));
        if (rs.getTimestamp("date_commande") != null) {
            c.setDateCommande(rs.getTimestamp("date_commande").toLocalDateTime());
        }
        String statutStr = rs.getString("statut");
        if (statutStr != null) {
            try {
                c.setStatut(Commande.Statut.valueOf(statutStr));
            } catch (IllegalArgumentException e) {
                c.setStatut(Commande.Statut.PANIER);
            }
        }
        c.setTotal(rs.getDouble("total"));
        c.setAdresseLivraison(rs.getString("adresse_livraison"));
        return c;
    }

    public static class ProduitVente {
        private int id;
        private String nom;
        private String unite;
        private int vendu;
        private double total;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getUnite() { return unite; }
        public void setUnite(String unite) { this.unite = unite; }
        public int getVendu() { return vendu; }
        public void setVendu(int vendu) { this.vendu = vendu; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }
}