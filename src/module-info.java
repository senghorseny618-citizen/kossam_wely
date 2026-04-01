module Kossam_welly {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
     requires java.sql;
     requires jbcrypt;
    requires org.json;
	requires jdk.jsobject;
	requires javafx.graphics;
	requires javafx.base;

    // OUVERTURE des packages vers javafx.fxml (pour l'injection FXML)
    opens application to javafx.fxml,javafx.graphics;
    opens controller to javafx.fxml;
    opens modele to javafx.base;

    // OUVERTURE pour la réflexion (nécessaire pour WebView/JSON)
    opens dao to Kossam_welly;
    opens service to Kossam_welly;

    // EXPORT pour que d'autres modules puissent utiliser ces packages
    exports application;
    exports modele;
    exports service;
}