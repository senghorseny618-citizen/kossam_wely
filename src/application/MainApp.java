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
    private static JavaBridge javaBridge;

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
            
            // Activer le bridge JavaScript
            webEngine.setJavaScriptEnabled(true);
            
            // Créer le bridge une fois
            javaBridge = new JavaBridge();

            // Attendre que la page soit chargée pour injecter le bridge
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    System.out.println("✅ Page chargée avec succès, injection du bridge Java...");
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("javaApp", javaBridge);
                        System.out.println("✅ Bridge Java injecté dans la page");
                        
                        // Tester si le bridge fonctionne
                        webEngine.executeScript("console.log('Bridge Java disponible: ' + (typeof javaApp !== 'undefined'));");
                        webEngine.executeScript("if(typeof javaApp !== 'undefined') { console.log('✅ javaApp est disponible dans la page'); }");
                        
                    } catch (Exception e) {
                        System.err.println("❌ Erreur lors de l'injection du bridge: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

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
                // Charger une page de test
                String testHtml = "<html><body><h1>Erreur</h1><p>Fichier " + page + " non trouvé</p></body></html>";
                webEngine.loadContent(testHtml);
            }

            Scene scene = new Scene(webView, 1200, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Kossam Welli - Dashboard " + role);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class JavaBridge {
        
        private final AuthService authService = AuthService.getInstance();
        private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
        private final ProduitDAO produitDAO = new ProduitDAO();
        private final CommandeDAO commandeDAO = new CommandeDAO();

        // ==================== MÉTHODES COMMUNES ====================
        
        public String getCurrentUser() {
            System.out.println("🔵 getCurrentUser appelé");
            if (authService.isLoggedIn()) {
                String json = authService.getCurrentUser().toJson();
                System.out.println("📋 Utilisateur: " + json);
                return json;
            }
            System.out.println("⚠️ Aucun utilisateur connecté");
            return "{}";
        }
        
        public void logout() {
            System.out.println("🔵 logout appelé");
            Platform.runLater(() -> {
                authService.logout();
                showLoginView();
            });
        }
     // ==================== MÉTHODES ADMIN POUR VALIDATION PRODUCTEUR ====================

        public String getProducteursEnAttente() {
            if (!authService.isAdmin()) return "[]";
            List<Utilisateur> producteurs = utilisateurDAO.findProducteursEnAttente();
            JSONArray jsonArray = new JSONArray();
            for (Utilisateur p : producteurs) {
                JSONObject obj = new JSONObject();
                obj.put("id", p.getId());
                obj.put("nom", p.getNom());
                obj.put("email", p.getEmail());
                obj.put("telephone", p.getTelephone());
                obj.put("adresse", p.getAdresse());
                obj.put("documents", p.getDocumentsJustificatifs() != null ? p.getDocumentsJustificatifs() : "");
                obj.put("dateDemande", p.getDateDemande() != null ? p.getDateDemande() : "");
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }

        public boolean validerProducteur(int userId, boolean valider, String commentaire) {
            if (!authService.isAdmin()) return false;
            return utilisateurDAO.validerProducteur(userId, valider, commentaire);
        }

        public boolean isProducteurValide(int userId) {
            return utilisateurDAO.isProducteurValide(userId);
        }
        
        // ==================== MÉTHODES PRODUCTEUR ====================
        
        public String getMyProducts() {
            System.out.println("🔵 getMyProducts appelé");
            if (!authService.isProducteur() && !authService.isAdmin()) {
                System.out.println("❌ Utilisateur non producteur");
                return "[]";
            }
            int producteurId = authService.getCurrentUser().getId();
            System.out.println("👤 ID Producteur: " + producteurId);
            
            List<Produit> produits = produitDAO.findByProducteur(producteurId);
            System.out.println("📦 Produits trouvés: " + produits.size());
            
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
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        public boolean createProduct(String produitJson) {
            System.out.println("🔵 createProduct appelé");
            System.out.println("📦 JSON reçu: " + produitJson);
            
            if (!authService.isProducteur()) {
                System.err.println("❌ Utilisateur non producteur");
                return false;
            }
            
            try {
                JSONObject json = new JSONObject(produitJson);
                
                String nom = json.optString("nom_produit");
                if (nom.isEmpty()) {
                    nom = json.optString("nom");
                }
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
                
                System.out.println("📦 Création du produit: " + p.getNom());
                int id = produitDAO.create(p);
                System.out.println("✅ Produit créé avec ID: " + id);
                
                return id > 0;
                
            } catch (Exception e) {
                System.err.println("❌ Erreur: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        
        public boolean updateProduct(String produitJson) {
            System.out.println("🔵 updateProduct appelé");
            System.out.println("📦 JSON reçu: " + produitJson);
            
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
                
                boolean result = produitDAO.update(p);
                System.out.println(result ? "✅ Produit modifié" : "❌ Échec modification");
                return result;
            } catch (Exception e) {
                System.err.println("❌ Erreur: " + e.getMessage());
                return false;
            }
        }
        
        public boolean toggleProductStatus(int productId, boolean disponible) {
            System.out.println("🔵 toggleProductStatus appelé: ID=" + productId + ", disponible=" + disponible);
            if (!authService.isProducteur()) return false;
            return produitDAO.toggleDisponible(productId, disponible);
        }
        
        public boolean deleteProduct(int productId) {
            System.out.println("🔵 deleteProduct appelé: ID=" + productId);
            if (!authService.isProducteur()) return false;
            return produitDAO.delete(productId);
        }
        
        public String getMyOrders() {
            System.out.println("🔵 getMyOrders appelé");
            if (!authService.isProducteur()) return "[]";
            int producteurId = authService.getCurrentUser().getId();
            List<Commande> commandes = commandeDAO.findByProducteur(producteurId);
            JSONArray jsonArray = new JSONArray();
            for (Commande c : commandes) {
                JSONObject obj = new JSONObject();
                obj.put("id_commande", c.getId());
                obj.put("client_nom", c.getClientNom() != null ? c.getClientNom() : "");
                obj.put("date_commande", c.getDateCommande() != null ? c.getDateCommande().toString() : "");
                obj.put("statut", c.getStatut() != null ? c.getStatut().name() : "PANIER");
                obj.put("montant_total", c.getTotal());
                
                JSONArray detailsArray = new JSONArray();
                if (c.getDetails() != null) {
                    for (DetailCommande d : c.getDetails()) {
                        JSONObject detailObj = new JSONObject();
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
            System.out.println("🔵 updateOrderStatus appelé: ID=" + orderId + ", status=" + status);
            if (!authService.isProducteur()) return false;
            return commandeDAO.updateStatus(orderId, status);
        }
        
        public String getMyStats() {
            System.out.println("🔵 getMyStats appelé");
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
                obj.put("nom_produit", pv.getNom());
                obj.put("total_quantite", pv.getVendu());
                obj.put("total_ventes", pv.getTotal());
                topVentesArray.put(obj);
            }
            stats.put("topVentes", topVentesArray);
            
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
                obj.put("prix", p.getPrix());
                obj.put("stock", p.getStock());
                obj.put("unite", p.getUnite());
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        public boolean createOrder(String panierJson) {
            System.out.println("🔵 createOrder appelé");
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
            System.out.println("🔵 getClientOrders appelé");
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
            System.out.println("🔵 getAllUsers appelé");
            if (!authService.isAdmin()) return "[]";
            List<Utilisateur> users = utilisateurDAO.findAll();
            JSONArray jsonArray = new JSONArray();
            for (Utilisateur u : users) {
                JSONObject obj = new JSONObject();
                obj.put("id", u.getId());
                obj.put("nom", u.getNom());
                obj.put("email", u.getEmail());
                obj.put("role", u.getRole().name());
                obj.put("active", u.isActive());
                jsonArray.put(obj);
            }
            return jsonArray.toString();
        }
        
        public boolean createUser(String userJson) {
            System.out.println("🔵 createUser appelé");
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
            System.out.println("🔵 toggleUserStatus appelé");
            if (!authService.isAdmin()) return false;
            return utilisateurDAO.toggleActive(userId, active);
        }
        
        public boolean deleteUser(int userId) {
            System.out.println("🔵 deleteUser appelé");
            if (!authService.isAdmin()) return false;
            return utilisateurDAO.toggleActive(userId, false);
        }
        
        public String getAllOrders() {
            System.out.println("🔵 getAllOrders appelé");
            if (!authService.isAdmin()) return "[]";
            return "[]";
        }
        
        public String getStats() {
            System.out.println("🔵 getStats appelé");
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