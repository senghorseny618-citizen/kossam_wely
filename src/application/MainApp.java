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
            
            webEngine.setJavaScriptEnabled(true);
            
            setupJavaScriptBridge();

            String page = switch(role.toUpperCase()) {
                case "ADMIN" -> "admin.html";
                case "PRODUCTEUR" -> "producteur.html";
                default -> "client.html";
            };

            java.net.URL url = MainApp.class.getResource("/resources/web/" + page);
            if (url != null) {
                System.out.println("📄 Chargement de la page: " + url.toExternalForm());
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
                System.out.println("✅ Page chargée, injection du bridge...");
                try {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaApp", new JavaBridge());
                    System.out.println("✅ Pont Java-JavaScript initialisé");
                } catch (Exception e) {
                    System.err.println("❌ Erreur injection bridge: " + e.getMessage());
                }
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
            System.out.println("🔵 getMyProducts appelé");
            
            if (!authService.isProducteur() && !authService.isAdmin()) {
                return "[]";
            }
            
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
                
                // Récupérer l'image ou mettre une image par défaut
                String imageUrl = p.getImageUrl();
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = getDefaultImage(p.getNom());
                }
                obj.put("image_url", imageUrl);
                
                jsonArray.put(obj);
            }
            
            System.out.println("📦 " + produits.size() + " produits retournés");
            return jsonArray.toString();
        }
        
        public boolean createProduct(String produitJson) {
            System.out.println("🔵 createProduct appelé: " + produitJson);
            
            if (!authService.isProducteur()) return false;
            
            try {
                JSONObject json = new JSONObject(produitJson);
                Produit p = new Produit();
                p.setNom(json.getString("nom_produit"));
                p.setDescription(json.optString("description", ""));
                p.setPrix(json.getDouble("prix"));
                p.setStock(json.getInt("stock"));
                p.setUnite(json.optString("unite", "unité"));
                p.setProducteurId(authService.getCurrentUser().getId());
                p.setDisponible(true);
                
                String imageUrl = json.optString("image_url", "");
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = getDefaultImage(p.getNom());
                }
                p.setImageUrl(imageUrl);
                
                int id = produitDAO.create(p);
                return id > 0;
            } catch (Exception e) {
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
                
                String imageUrl = json.optString("image_url", "");
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = getDefaultImage(p.getNom());
                }
                p.setImageUrl(imageUrl);
                
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
                obj.put("client_nom", c.getClientNom() != null ? c.getClientNom() : "");
                obj.put("date_commande", c.getDateCommande() != null ? c.getDateCommande().toString() : "");
                obj.put("statut", c.getStatut() != null ? c.getStatut().name() : "PAYEE");
                obj.put("montant_total", c.getTotal());
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
            return stats.toString();
        }
        
        // ==================== MÉTHODES CLIENT ====================
        
        public String getAllProducts() {
            System.out.println("🔵 getAllProducts appelé");
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
                obj.put("disponible", p.isDisponible());
                
                String imageUrl = p.getImageUrl();
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = getDefaultImage(p.getNom());
                }
                obj.put("image_url", imageUrl);
                
                jsonArray.put(obj);
            }
            System.out.println("📦 " + produits.size() + " produits retournés au client");
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
                obj.put("role", u.getRole().name());
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
        
        public String getStats() {
            if (!authService.isAdmin()) return "{}";
            JSONObject stats = new JSONObject();
            stats.put("totalUsers", utilisateurDAO.findAll().size());
            stats.put("totalProducteurs", utilisateurDAO.findAll().stream()
                    .filter(u -> u.getRole() == Utilisateur.Role.PRODUCTEUR).count());
            stats.put("totalProduits", produitDAO.findAllAvailable().size());
            return stats.toString();
        }
        
        // ==================== MÉTHODE POUR IMAGES PAR DÉFAUT ====================
        
        private String getDefaultImage(String nomProduit) {
            String nom = nomProduit.toLowerCase();
            if (nom.contains("lait")) {
                return "https://cdn-icons-png.flaticon.com/512/2965/2965307.png";
            } else if (nom.contains("fromage")) {
                return "https://cdn-icons-png.flaticon.com/512/1998/1998629.png";
            } else if (nom.contains("yaourt") || nom.contains("yogourt") || nom.contains("thiakry")) {
                return "https://cdn-icons-png.flaticon.com/512/3082/3082382.png";
            } else if (nom.contains("beurre")) {
                return "https://cdn-icons-png.flaticon.com/512/2972/2972673.png";
            } else {
                return "https://cdn-icons-png.flaticon.com/512/2965/2965307.png";
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}