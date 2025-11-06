package com.farmmanager;

import javafx.application.Application;
import javafx.application.Platform; // NOVO IMPORT
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import com.farmmanager.util.AlertUtil;
import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;

/**
 * Classe principal do JavaFX.
 * Configura e exibe a cena principal (Stage).
 * ATUALIZADO: Agora define a janela para ser maximizada ao iniciar.
 * ATUALIZADO: Adiciona o ícone da aplicação na barra de título da janela.
 * ATUALIZADO: Armazena a instância do Stage principal (primaryStage)
 * para referência global e permite focar a janela.
 * ATUALIZADO: Chama Database.initDb() DEPOIS da inicialização do JavaFX.
 */
public class App extends Application {

    // NOVO: Instância estática para o Stage principal
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        // NOVO: Armazena a referência ao Stage principal
        primaryStage = stage; 
        
        // NOVO: Carrega o ícone para ser usado nos diálogos
        AlertUtil.loadAppIcon();

        // --- ATUALIZAÇÃO CRÍTICA ---
        // Inicializa o banco de dados AQUI.
        // Se falhar, o AlertUtil.showError() (que é JavaFX)
        // funcionará corretamente, pois o Toolkit já está inicializado.
        try {
            com.farmmanager.model.Database.initDb();
        } catch (Exception e) {
            // Se o initDb() falhar (ex: AlertUtil.showError), 
            // a aplicação não deve continuar.
            System.err.println("Falha fatal na inicialização do banco de dados. A fechar.");
            Platform.exit(); // Fecha a aplicação JavaFX
            System.exit(1); // Fecha o processo
            return; // Interrompe a execução do 'start'
        }
        // --- FIM DA ATUALIZAÇÃO ---

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

    /**
     * NOVO: Método de acesso global ao Stage principal.
     * @return O Stage principal da aplicação.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * NOVO: Traz a janela principal para a frente e a foca.
     * Deve ser chamado usando Platform.runLater().
     */
    public static void focusStage() {
        if (primaryStage != null) {
            Platform.runLater(() -> {
                primaryStage.toFront();
                primaryStage.requestFocus();
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}