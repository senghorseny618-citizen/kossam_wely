package application;

public class Launcher {
    public static void main(String[] args) {
        // Vérifier que JavaFX est disponible
        try {
            Class.forName("javafx.application.Application");
            System.out.println("✅ JavaFX est disponible");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERREUR: JavaFX n'est pas dans le classpath!");
            System.err.println("Assurez-vous d'avoir ajouté les JARs JavaFX dans le module-path");
            e.printStackTrace();
            return;
        }
        
        MainApp.main(args);
    }
}