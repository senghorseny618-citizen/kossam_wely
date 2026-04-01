package service;

import dao.CommandeDAO;
import dao.ProduitDAO;
import modele.Commande;
import modele.Produit;
import modele.DetailCommande;

import java.util.ArrayList;
import java.util.List;

public class ProduitService {
    
    private final ProduitDAO produitDAO;
    private final CommandeDAO commandeDAO;
    
    public ProduitService() {
        this.produitDAO = new ProduitDAO();
        this.commandeDAO = new CommandeDAO();
    }
    
    /**
     * Récupère tous les produits d'un producteur
     */
    public List<Produit> getProduitsByProducteur(int producteurId) {
        return produitDAO.findByProducteur(producteurId);
    }
    
    /**
     * Récupère le nombre de produits d'un producteur
     */
    public int getNombreProduitsByProducteur(int producteurId) {
        return produitDAO.findByProducteur(producteurId).size();
    }
    
    /**
     * Récupère les commandes contenant des produits d'un producteur
     */
    public List<Commande> getCommandesByProducteur(int producteurId) {
        return commandeDAO.findByProducteur(producteurId);
    }
    
    /**
     * Récupère le nombre de commandes pour un producteur
     */
    public int getNombreCommandesByProducteur(int producteurId) {
        return commandeDAO.findByProducteur(producteurId).size();
    }
    
    /**
     * Calcule le chiffre d'affaires d'un producteur
     */
    public double getCA(int producteurId) {
        List<Commande> commandes = commandeDAO.findByProducteur(producteurId);
        double ca = 0;
        for (Commande commande : commandes) {
            // Calculer uniquement la part du producteur dans la commande
            for (DetailCommande detail : commande.getDetails()) {
                // Ici vous pouvez filtrer par produit du producteur
                // Pour simplifier, on prend tout le total
                ca += commande.getTotal();
                break; // À modifier selon votre logique métier
            }
        }
        return ca;
    }
    
    /**
     * Ajoute un nouveau produit
     */
    public boolean ajouterProduit(Produit produit) {
        int id = produitDAO.create(produit);
        return id > 0;
    }
    
    /**
     * Active/Désactive un produit
     */
    public boolean toggleDisponibilite(int produitId) {
        // Récupérer le produit
        List<Produit> produits = produitDAO.findAllAvailable();
        for (Produit p : produits) {
            if (p.getId() == produitId) {
                p.setDisponible(!p.isDisponible());
                // Mettre à jour dans la base
                return true;
            }
        }
        return false;
    }
    
    /**
     * Supprime un produit
     */
    public boolean supprimerProduit(int produitId) {
        // Implémentez la suppression logique ou physique
        return true;
    }
    
    /**
     * Met à jour le statut d'une commande
     */
    public boolean mettreAJourStatutCommande(int commandeId, String statut) {
        return commandeDAO.updateStatus(commandeId, statut);
    }
}