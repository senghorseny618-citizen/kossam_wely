package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import dao.DatabaseConnection;
import dao.ProduitDAO;
import dao.UtilisateurDAO;
import dao.CommandeDAO;
import service.AuthService;
import modele.Utilisateur;
import modele.Produit;
import modele.Commande;
import modele.DetailCommande;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static WebView webView;
    private static WebEngine webEngine;

    @Override
    public void start(Stage primaryStage) {
        MainApp.primaryStage = primaryStage;

        // Test connexion BDD au démarrage
        if (!DatabaseConnection.testConnection()) {
            System.err.println("⚠️ Impossible de se connecter à la base de données");
        }

        showLoginView();

        primaryStage.setTitle("Kossam welli - Connexion");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(750);
        primaryStage.setWidth(1300);
        primaryStage.setHeight(850);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/resources/view/login.fxml"));
            BorderPane loginPane = loader.load();
            
            Scene scene = new Scene(loginPane);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Kossam Welli - Connexion");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur chargement login.fxml : " + e.getMessage());
        }
    }

    public static void showRegisterView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/resources/view/register.fxml"));

            loader.setControllerFactory(param -> {
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            BorderPane registerPane = loader.load();
            Scene scene = new Scene(registerPane, 900, 700);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Kossam welli - Inscription");
            primaryStage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur chargement register.fxml : " + e.getMessage());
        }
    }

    public static void showDashboard(String role) {
        try {
            webView = new WebView();
            webEngine = webView.getEngine();

            setupJavaScriptBridge();

            String page = switch(role.toUpperCase()) {
                case "ADMIN" -> "admin.html";
                case "PRODUCTEUR" -> "producteur.html";
                default -> "client.html";
            };

            java.net.URL url = MainApp.class.getResource("/resources/web/" + page);
            if (url != null) {
                webEngine.load(url.toExternalForm());
            } else {
                System.err.println("❌ Fichier non trouvé : /resources/web/" + page);
            }

            Scene scene = new Scene(webView, 1200, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Kossam Welli - Dashboard " + role);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupJavaScriptBridge() {
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", new JavaBridge());
                System.out.println("✅ Pont Java-JavaScript initialisé");
            }
        });
    }

    public static class JavaBridge {
        
        private final AuthService authService = AuthService.getInstance();
        private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
        private final ProduitDAO produitDAO = new ProduitDAO();
        private final CommandeDAO commandeDAO = new CommandeDAO();

        // ==================== MÉTHODES COMMUNES ====================
        
        public String getCurrentUser() {
            if (authService.isLoggedIn()) {
                return authService.getCurrentUser().toJson();
            }
            return "{}";
        }
        
        public void logout() {
            Platform.runLater(() -> {
                authService.logout();
                showLoginView();
            });
        }
        
        // ==================== MÉTHODES PRODUCTEUR ====================
        
        public String getMyProducts() {
            if (!authService.isProducteur() && !authService.isAdmin()) return "[]";
            int producteurId = authService.getCurrentUser().getId();
            List<Produit> produits = produitDAO.findByProducteur(producteurId);
            JSONArray jsonArray = new JSONArray();
            for (Produit p : produits) {
                JSONObject obj = new JSONObject();
                obj.put("id_produit", p.getId());
                obj.put("nom_produit", p.getNom());
                obj.put("description", p.getDescription() != null ? p.getDescription() : "");
                obj.put("prix", p.getPrix());
                obj.put("stock", p.getStock());
                obj.put("unite", p.getUnite());
                obj.put("disponible", p.isDisponible());
                obj.put("image_url", p.getImageUrl() != null ? p.getImageUrl() : "");
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        public boolean createProduct(String produitJson) {
            System.out.println("=== createProduct appelé ===");
            System.out.println("JSON reçu: " + produitJson);
            
            if (!authService.isProducteur()) {
                System.err.println("❌ L'utilisateur n'est pas un producteur !");
                return false;
            }
            
            try {
                JSONObject json = new JSONObject(produitJson);
                
                String nom = json.optString("nom_produit", json.optString("nom", ""));
                if (nom.isEmpty()) {
                    System.err.println("❌ Nom du produit manquant");
                    return false;
                }
                
                String description = json.optString("description", "");
                double prix = json.optDouble("prix", 0);
                int stock = json.optInt("stock", 0);
                String unite = json.optString("unite", "unité");
                
                if (prix <= 0) {
                    System.err.println("❌ Prix invalide: " + prix);
                    return false;
                }
                
                Produit p = new Produit();
                p.setNom(nom);
                p.setDescription(description);
                p.setPrix(prix);
                p.setStock(stock);
                p.setUnite(unite);
                p.setProducteurId(authService.getCurrentUser().getId());
                p.setDisponible(true);
                
                int id = produitDAO.create(p);
                System.out.println("✅ Produit créé avec ID: " + id);
                
                return id > 0;
                
            } catch (Exception e) {
                System.err.println("❌ Erreur dans createProduct: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        
        public boolean updateProduct(String produitJson) {
            if (!authService.isProducteur()) return false;
            try {
                JSONObject json = new JSONObject(produitJson);
                Produit p = new Produit();
                p.setId(json.getInt("id_produit"));
                p.setNom(json.getString("nom_produit"));
                p.setDescription(json.optString("description", ""));
                p.setPrix(json.getDouble("prix"));
                p.setStock(json.getInt("stock"));
                p.setUnite(json.optString("unite", "unité"));
                p.setProducteurId(authService.getCurrentUser().getId());
                p.setDisponible(json.optBoolean("disponible", true));
                p.setImageUrl(json.optString("image_url", ""));
                
                return produitDAO.update(p);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        public boolean toggleProductStatus(int productId, boolean disponible) {
            if (!authService.isProducteur()) return false;
            return produitDAO.toggleDisponible(productId, disponible);
        }
        
        public boolean deleteProduct(int productId) {
            if (!authService.isProducteur()) return false;
            return produitDAO.delete(productId);
        }
        
        public String getMyOrders() {
            if (!authService.isProducteur()) return "[]";
            int producteurId = authService.getCurrentUser().getId();
            List<Commande> commandes = commandeDAO.findByProducteur(producteurId);
            JSONArray jsonArray = new JSONArray();
            for (Commande c : commandes) {
                JSONObject obj = new JSONObject();
                obj.put("id_commande", c.getId());
                obj.put("client_id", c.getClientId());
                obj.put("client_nom", c.getClientNom() != null ? c.getClientNom() : "");
                obj.put("date_commande", c.getDateCommande() != null ? c.getDateCommande().toString() : "");
                obj.put("statut", c.getStatut() != null ? c.getStatut().name() : "PANIER");
                obj.put("montant_total", c.getTotal());
                obj.put("adresse_livraison", c.getAdresseLivraison() != null ? c.getAdresseLivraison() : "");
                
                JSONArray detailsArray = new JSONArray();
                if (c.getDetails() != null) {
                    for (DetailCommande d : c.getDetails()) {
                        JSONObject detailObj = new JSONObject();
                        detailObj.put("id_detail", d.getId());
                        detailObj.put("produit_id", d.getProduitId());
                        detailObj.put("nom_produit", d.getNomProduit());
                        detailObj.put("quantite", d.getQuantite());
                        detailObj.put("prix", d.getPrixUnitaire());
                        detailsArray.put(detailObj);
                    }
                }
                obj.put("details", detailsArray);
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        public boolean updateOrderStatus(int orderId, String status) {
            if (!authService.isProducteur()) return false;
            return commandeDAO.updateStatus(orderId, status);
        }
        
        public String getMyStats() {
            if (!authService.isProducteur()) return "{}";
            int producteurId = authService.getCurrentUser().getId();
            
            JSONObject stats = new JSONObject();
            stats.put("totalProduits", produitDAO.countByProducteur(producteurId));
            stats.put("totalCommandes", commandeDAO.countByProducteur(producteurId));
            stats.put("ca", commandeDAO.getCA(producteurId));
            
            List<CommandeDAO.ProduitVente> topVentes = commandeDAO.getTopVentes(producteurId, 5);
            JSONArray topVentesArray = new JSONArray();
            for (CommandeDAO.ProduitVente pv : topVentes) {
                JSONObject obj = new JSONObject();
                obj.put("id_produit", pv.getId());
                obj.put("nom_produit", pv.getNom());
                obj.put("unite", pv.getUnite());
                obj.put("total_quantite", pv.getVendu());
                obj.put("total_ventes", pv.getTotal());
                topVentesArray.put(obj);
            }
            stats.put("topVentes", topVentesArray);
            
            return stats.toString();
        }
        
        // ==================== MÉTHODES CLIENT ====================
        
        public String getAllProducts() {
            List<Produit> produits = produitDAO.findAllAvailable();
            JSONArray jsonArray = new JSONArray();
            for (Produit p : produits) {
                JSONObject obj = new JSONObject();
                obj.put("id", p.getId());
                obj.put("nom", p.getNom());
                obj.put("description", p.getDescription() != null ? p.getDescription() : "");
                obj.put("prix", p.getPrix());
                obj.put("stock", p.getStock());
                obj.put("unite", p.getUnite());
                obj.put("producteur_id", p.getProducteurId());
                obj.put("producteur_nom", p.getProducteurNom() != null ? p.getProducteurNom() : "");
                obj.put("disponible", p.isDisponible());
                obj.put("image_url", p.getImageUrl() != null ? p.getImageUrl() : "");
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        public boolean createOrder(String panierJson) {
            if (!authService.isClient()) return false;
            try {
                JSONArray panier = new JSONArray(panierJson);
                Utilisateur client = authService.getCurrentUser();
                
                Commande commande = new Commande();
                commande.setClientId(client.getId());
                commande.setClientNom(client.getNom());
                commande.setAdresseLivraison(client.getAdresse());
                commande.setStatut(Commande.Statut.PAYEE);
                
                for (int i = 0; i < panier.length(); i++) {
                    JSONObject item = panier.getJSONObject(i);
                    DetailCommande detail = new DetailCommande();
                    detail.setProduitId(item.getInt("id"));
                    detail.setNomProduit(item.getString("nom"));
                    detail.setQuantite(item.getInt("quantite"));
                    detail.setPrixUnitaire(item.getDouble("prixUnitaire"));
                    commande.getDetails().add(detail);
                }
                
                commande.calculerTotal();
                int id = commandeDAO.create(commande);
                
                // Mettre à jour le stock des produits
                for (int i = 0; i < panier.length(); i++) {
                    JSONObject item = panier.getJSONObject(i);
                    Produit produit = produitDAO.findById(item.getInt("id"));
                    if (produit != null) {
                        int nouveauStock = produit.getStock() - item.getInt("quantite");
                        produitDAO.updateStock(produit.getId(), nouveauStock);
                    }
                }
                
                return id > 0;
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        public String getClientOrders() {
            if (!authService.isClient()) return "[]";
            int clientId = authService.getCurrentUser().getId();
            List<Commande> commandes = commandeDAO.findByClient(clientId);
            JSONArray jsonArray = new JSONArray();
            for (Commande c : commandes) {
                JSONObject obj = new JSONObject();
                obj.put("id", c.getId());
                obj.put("dateCommande", c.getDateCommande() != null ? c.getDateCommande().toString() : "");
                obj.put("statut", c.getStatut() != null ? c.getStatut().getLibelle() : "");
                obj.put("total", c.getTotal());
                obj.put("adresseLivraison", c.getAdresseLivraison());
                
                JSONArray detailsArray = new JSONArray();
                for (DetailCommande d : c.getDetails()) {
                    JSONObject detailObj = new JSONObject();
                    detailObj.put("id", d.getId());
                    detailObj.put("nomProduit", d.getNomProduit());
                    detailObj.put("quantite", d.getQuantite());
                    detailObj.put("prixUnitaire", d.getPrixUnitaire());
                    detailsArray.put(detailObj);
                }
                obj.put("details", detailsArray);
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        // ==================== MÉTHODES ADMIN ====================
        
        public String getAllUsers() {
            if (!authService.isAdmin()) return "[]";
            List<Utilisateur> users = utilisateurDAO.findAll();
            JSONArray jsonArray = new JSONArray();
            for (Utilisateur u : users) {
                JSONObject obj = new JSONObject();
                obj.put("id", u.getId());
                obj.put("nom", u.getNom());
                obj.put("email", u.getEmail());
                obj.put("telephone", u.getTelephone() != null ? u.getTelephone() : "");
                obj.put("adresse", u.getAdresse() != null ? u.getAdresse() : "");
                obj.put("role", u.getRole().name());
                obj.put("active", u.isActive());
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        public boolean createUser(String userJson) {
            if (!authService.isAdmin()) return false;
            try {
                JSONObject json = new JSONObject(userJson);
                Utilisateur user = new Utilisateur();
                user.setNom(json.getString("nom"));
                user.setEmail(json.getString("email"));
                user.setMotDePasse(json.getString("password"));
                user.setTelephone(json.optString("telephone", ""));
                user.setAdresse(json.optString("adresse", ""));
                user.setRole(Utilisateur.Role.valueOf(json.getString("role")));
                user.setActive(true);
                
                int id = utilisateurDAO.create(user);
                return id > 0;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        public boolean toggleUserStatus(int userId, boolean active) {
            if (!authService.isAdmin()) return false;
            return utilisateurDAO.toggleActive(userId, active);
        }
        
        public boolean deleteUser(int userId) {
            if (!authService.isAdmin()) return false;
            return utilisateurDAO.toggleActive(userId, false);
        }
        
        public String getAllOrders() {
            if (!authService.isAdmin()) return "[]";
            JSONArray jsonArray = new JSONArray();
            return jsonArray.toString();
        }
        
        public String getStats() {
            if (!authService.isAdmin()) return "{}";
            JSONObject stats = new JSONObject();
            stats.put("totalUsers", utilisateurDAO.findAll().size());
            stats.put("totalProducteurs", utilisateurDAO.findAll().stream()
                    .filter(u -> u.getRole() == Utilisateur.Role.PRODUCTEUR).count());
            stats.put("totalProduits", produitDAO.findAllAvailable().size());
            stats.put("totalCommandes", 0);
            return stats.toString();
        }
    }

    public static WebEngine getWebEngine() { return webEngine; }
    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}