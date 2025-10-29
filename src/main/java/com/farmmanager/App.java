package com.farmmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Classe principal do JavaFX.
 * Configura e exibe a cena principal (Stage).
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Constrói o caminho para o arquivo FXML dentro do pacote de resources
        URL fxmlUrl = getClass().getResource("/com/farmmanager/MainView.fxml");
        if (fxmlUrl == null) {
            System.err.println("Erro: Não foi possível encontrar MainView.fxml");
            return;
        }

        Parent root = FXMLLoader.load(fxmlUrl);
        Scene scene = new Scene(root, 1024, 768); // Tamanho da janela

        // Constrói o caminho para o arquivo CSS
        URL cssUrl = getClass().getResource("/com/farmmanager/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Aviso: Não foi possível encontrar styles.css");
        }

        stage.setTitle("FarmManager - Sistema de Gestão de Fazenda");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
