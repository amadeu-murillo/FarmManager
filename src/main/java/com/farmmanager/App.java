package com.farmmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
// NOVO: Imports necessários para obter o tamanho da tela
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import com.farmmanager.util.AlertUtil; // NOVO IMPORT
import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;

/**
 * Classe principal do JavaFX.
 * Configura e exibe a cena principal (Stage).
 * ATUALIZADO: Agora define a janela para ser maximizada ao iniciar.
 * ATUALIZADO: Adiciona o ícone da aplicação na barra de título da janela.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // NOVO: Carrega o ícone para ser usado nos diálogos
        AlertUtil.loadAppIcon();

        // Constrói o caminho para o arquivo FXML dentro do pacote de resources
        URL fxmlUrl = getClass().getResource("/com/farmmanager/MainView.fxml");
        if (fxmlUrl == null) {
            System.err.println("Erro: Não foi possível encontrar MainView.fxml");
            return;
        }

        Parent root = FXMLLoader.load(fxmlUrl);
        
        // --- ATUALIZAÇÃO: Obter o tamanho da tela para uma melhor inicialização ---
        // Pega os limites da tela principal
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        // Define o tamanho inicial da cena para o tamanho da tela
        // Isso evita que a janela "pule" de 1024x768 para maximizada
        Scene scene = new Scene(root, primaryScreenBounds.getWidth(), primaryScreenBounds.getHeight()); 

        // Constrói o caminho para o arquivo CSS
        URL cssUrl = getClass().getResource("/com/farmmanager/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Aviso: Não foi possível encontrar styles.css");
        }

        stage.setTitle("FarmManager - Sistema de Gestão de Fazenda");
        stage.setScene(scene);
        
        // --- NOVO: Adiciona o ícone da aplicação ---
        // O ícone deve estar na pasta 'resources/com/farmmanager/'
        try {
            // Tenta carregar a imagem 'logo.png' da pasta de resources
            Image appIcon = new Image(getClass().getResourceAsStream("/com/farmmanager/logo.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            // Informa se o logo não for encontrado, mas não impede a aplicação de rodar
            System.err.println("Aviso: Não foi possível carregar o ícone da aplicação 'logo.png'. " + e.getMessage());
        }
        
        // --- NOVO: Maximiza a janela ---
        // Define o estado da janela como maximizado
        stage.setMaximized(true);
        
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

